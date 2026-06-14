package Zero_Knowledge_Vault.domain.share.service;

import Zero_Knowledge_Vault.domain.share.dto.request.RewrapItemRequest;
import Zero_Knowledge_Vault.domain.share.dto.request.RewrapShareKeyRequest;
import Zero_Knowledge_Vault.domain.share.dto.response.RewrapCandidateResponse;
import Zero_Knowledge_Vault.domain.share.dto.response.RewrapShareKeyResponse;
import Zero_Knowledge_Vault.domain.share.entity.MemberShareKey;
import Zero_Knowledge_Vault.domain.share.policy.ShareKeyPolicy;
import Zero_Knowledge_Vault.domain.share.repository.MemberShareKeyRepository;
import Zero_Knowledge_Vault.domain.share.type.ShareKeyStatus;
import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItem;
import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemMember;
import Zero_Knowledge_Vault.domain.shareditem.repository.SharedItemMemberRepository;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemMemberStatus;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemStatus;
import Zero_Knowledge_Vault.global.exception.custom.ShareKeyException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberShareKeyRewrapService {

    private static final int MAX_CIPHER_TEXT_LENGTH = 1_000_000;

    private final MemberShareKeyRepository memberShareKeyRepository;
    private final SharedItemMemberRepository sharedItemMemberRepository;
    private final ShareKeyPolicy shareKeyPolicy;

    public List<RewrapCandidateResponse> getRewrapCandidates(Long memberId) {
        MemberShareKey activeShareKey = findActiveShareKey(memberId);
        return sharedItemMemberRepository
                .findAccessibleItems(memberId, SharedItemMemberStatus.ACTIVE, SharedItemStatus.ACTIVE)
                .stream()
                .filter(member -> member.getEncryptedItemKeyBase64() != null && !member.getEncryptedItemKeyBase64().isBlank())
                .filter(member -> member.getRecipientKeyVersion() != null
                        && member.getRecipientKeyVersion() < activeShareKey.getKeyVersion())
                .map(member -> RewrapCandidateResponse.from(member, activeShareKey))
                .toList();
    }

    @Transactional
    public RewrapShareKeyResponse rewrapShareKey(Long memberId, RewrapShareKeyRequest request) {
        validateRequest(request);

        MemberShareKey activeShareKey = findActiveShareKey(memberId);
        if (!activeShareKey.getKeyVersion().equals(request.targetKeyVersion())) {
            throw conflict(
                    "SHARE_KEY_REWRAP_TARGET_VERSION_CONFLICT",
                    "targetKeyVersion does not match the caller's ACTIVE share key"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        Set<Long> seenSharedItemIds = new HashSet<>();

        for (RewrapItemRequest itemRequest : request.items()) {
            if (!seenSharedItemIds.add(itemRequest.sharedItemId())) {
                throw badRequest("DUPLICATE_REWRAP_ITEM", "Duplicate sharedItemId in rewrap items");
            }

            SharedItemMember member = sharedItemMemberRepository
                    .findBySharedItemSharedItemIdAndMemberMemberIdAndStatus(
                            itemRequest.sharedItemId(),
                            memberId,
                            SharedItemMemberStatus.ACTIVE
                    )
                    .orElseThrow(() -> forbidden(
                            "SHARED_ITEM_ACCESS_DENIED",
                            "Shared item access denied"
                    ));

            SharedItem item = member.getSharedItem();
            requireActiveItem(item);

            if (!item.getMembershipVersion().equals(itemRequest.expectedMembershipVersion())) {
                throw conflict(
                        "SHARED_ITEM_MEMBERSHIP_VERSION_CONFLICT",
                        "Shared item membershipVersion conflict"
                );
            }

            if (member.getRecipientKeyVersion() == null
                    || member.getRecipientKeyVersion() >= request.targetKeyVersion()) {
                throw conflict(
                        "SHARED_ITEM_REWRAP_NOT_REQUIRED",
                        "Shared item wrapper is already up to date"
                );
            }

            validateCipherText(itemRequest.encryptedItemKeyBase64());

            int updated = sharedItemMemberRepository.updateEncryptedItemKey(
                    itemRequest.sharedItemId(),
                    memberId,
                    itemRequest.encryptedItemKeyBase64(),
                    request.targetKeyVersion(),
                    now,
                    SharedItemMemberStatus.ACTIVE
            );

            if (updated == 0) {
                throw conflict(
                        "SHARED_ITEM_MEMBER_UPDATE_CONFLICT",
                        "Shared item member wrapper update conflicted"
                );
            }
        }

        return new RewrapShareKeyResponse(
                request.targetKeyVersion(),
                request.items().size(),
                now
        );
    }

    private MemberShareKey findActiveShareKey(Long memberId) {
        return memberShareKeyRepository
                .findTopByMemberMemberIdAndStatusOrderByKeyVersionDesc(memberId, ShareKeyStatus.ACTIVE)
                .orElseThrow(() -> notFound(
                        "MEMBER_SHARE_KEY_NOT_FOUND",
                        "Active member share key not found"
                ));
    }

    private void requireActiveItem(SharedItem item) {
        if (item.getStatus() != SharedItemStatus.ACTIVE) {
            throw notFound("SHARED_ITEM_NOT_FOUND", "Shared item not found");
        }
    }

    private void validateRequest(RewrapShareKeyRequest request) {
        if (request == null) {
            throw badRequest("INVALID_REWRAP_REQUEST", "Request body is required");
        }
        if (request.targetKeyVersion() == null) {
            throw badRequest("INVALID_REWRAP_REQUEST", "targetKeyVersion is required");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw badRequest("INVALID_REWRAP_REQUEST", "items are required");
        }
        for (RewrapItemRequest item : request.items()) {
            if (item == null
                    || item.sharedItemId() == null
                    || item.expectedMembershipVersion() == null
                    || item.encryptedItemKeyBase64() == null
                    || item.encryptedItemKeyBase64().isBlank()) {
                throw badRequest("INVALID_REWRAP_REQUEST", "Each rewrap item requires sharedItemId, expectedMembershipVersion, and encryptedItemKeyBase64");
            }
        }
    }

    private void validateCipherText(String value) {
        if (value == null || value.isBlank()) {
            throw badRequest("INVALID_REWRAP_ITEM_BASE64", "encryptedItemKeyBase64 is required");
        }

        if (value.length() > Math.min(MAX_CIPHER_TEXT_LENGTH, shareKeyPolicy.maxKeyTextLength())) {
            throw badRequest("INVALID_REWRAP_ITEM_BASE64", "encryptedItemKeyBase64 is too long");
        }

        try {
            Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw badRequest("INVALID_REWRAP_ITEM_BASE64", "encryptedItemKeyBase64 must be valid Base64");
        }
    }

    private ShareKeyException badRequest(String code, String message) {
        return new ShareKeyException(HttpStatus.BAD_REQUEST, code, message);
    }

    private ShareKeyException forbidden(String code, String message) {
        return new ShareKeyException(HttpStatus.FORBIDDEN, code, message);
    }

    private ShareKeyException notFound(String code, String message) {
        return new ShareKeyException(HttpStatus.NOT_FOUND, code, message);
    }

    private ShareKeyException conflict(String code, String message) {
        return new ShareKeyException(HttpStatus.CONFLICT, code, message);
    }
}
