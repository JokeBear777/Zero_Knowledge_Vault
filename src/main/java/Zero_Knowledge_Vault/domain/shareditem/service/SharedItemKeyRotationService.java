package Zero_Knowledge_Vault.domain.shareditem.service;

import Zero_Knowledge_Vault.domain.share.entity.MemberShareKey;
import Zero_Knowledge_Vault.domain.share.repository.MemberShareKeyRepository;
import Zero_Knowledge_Vault.domain.share.type.ShareKeyStatus;
import Zero_Knowledge_Vault.domain.shareditem.dto.request.MemberKeyWrapperRequest;
import Zero_Knowledge_Vault.domain.shareditem.dto.request.RotateSharedItemKeyRequest;
import Zero_Knowledge_Vault.domain.shareditem.dto.response.RotationMemberResponse;
import Zero_Knowledge_Vault.domain.shareditem.dto.response.SharedItemRotationContextResponse;
import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItem;
import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemMember;
import Zero_Knowledge_Vault.domain.shareditem.repository.SharedItemMemberRepository;
import Zero_Knowledge_Vault.domain.shareditem.repository.SharedItemRepository;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemMemberRole;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemMemberStatus;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemStatus;
import Zero_Knowledge_Vault.global.exception.custom.SharedItemException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SharedItemKeyRotationService {

    private static final int MAX_CIPHER_TEXT_LENGTH = 1_000_000;

    private final MemberShareKeyRepository memberShareKeyRepository;
    private final SharedItemRepository sharedItemRepository;
    private final SharedItemMemberRepository sharedItemMemberRepository;

    public SharedItemRotationContextResponse getRotationContext(Long requesterId, Long sharedItemId) {
        SharedItem sharedItem = findSharedItem(sharedItemId);
        requireActiveSharedItem(sharedItem);

        List<SharedItemMember> members = sharedItemMemberRepository
                .findBySharedItemSharedItemIdOrderByCreatedAtAsc(sharedItemId);
        SharedItemMember requester = requireActiveMembership(members, requesterId);
        requireOwner(requester);

        List<RotationMemberResponse> rotationMembers = members.stream()
                .filter(member -> member.getStatus() == SharedItemMemberStatus.ACTIVE)
                .map(this::toRotationMemberResponse)
                .toList();

        return new SharedItemRotationContextResponse(
                sharedItem.getSharedItemId(),
                sharedItem.getVersion(),
                sharedItem.getKeyVersion(),
                rotationMembers
        );
    }

    @Transactional
    public void rotateKey(Long requesterId, Long sharedItemId, RotateSharedItemKeyRequest request) {
        validateRotateRequest(request);

        SharedItem sharedItem = findSharedItem(sharedItemId);
        requireActiveSharedItem(sharedItem);

        List<SharedItemMember> members = sharedItemMemberRepository
                .findBySharedItemSharedItemIdOrderByCreatedAtAsc(sharedItemId);
        SharedItemMember requester = requireActiveMembership(members, requesterId);
        requireOwner(requester);

        validateExpectedVersion(sharedItem, request.expectedVersion(), request.expectedKeyVersion());

        Set<Long> revokedMemberIds = normalizeRevokedMemberIds(request.revokedMemberIds());
        validateRevokedMembers(members, revokedMemberIds);

        List<SharedItemMember> remainingActiveMembers = members.stream()
                .filter(member -> member.getStatus() == SharedItemMemberStatus.ACTIVE)
                .filter(member -> !revokedMemberIds.contains(member.getMember().getMemberId()))
                .toList();
        validateRemainingActiveMembers(remainingActiveMembers);

        Map<Long, MemberKeyWrapperRequest> wrappersByMemberId = validateWrapperCompleteness(
                remainingActiveMembers,
                request.memberKeyWrappers(),
                revokedMemberIds
        );
        validateRecipientKeyVersions(remainingActiveMembers, wrappersByMemberId);

        LocalDateTime now = LocalDateTime.now();
        int rotated = sharedItemRepository.rotateCipherIfVersionAndKeyVersionMatch(
                sharedItemId,
                request.expectedVersion(),
                request.expectedKeyVersion(),
                request.titleCipherBase64(),
                request.itemCipherBase64(),
                now,
                SharedItemStatus.ACTIVE
        );

        if (rotated == 0) {
            throw conflict("SHARED_ITEM_VERSION_CONFLICT", "Shared item version or keyVersion conflict");
        }

        for (MemberKeyWrapperRequest wrapper : wrappersByMemberId.values()) {
            int updated = sharedItemMemberRepository.updateEncryptedItemKey(
                    sharedItemId,
                    wrapper.memberId(),
                    wrapper.encryptedItemKeyBase64(),
                    wrapper.recipientKeyVersion(),
                    now,
                    SharedItemMemberStatus.ACTIVE
            );
            if (updated == 0) {
                throw conflict("SHARED_ITEM_MEMBER_UPDATE_CONFLICT", "Shared item member wrapper update conflicted");
            }
        }

        if (!revokedMemberIds.isEmpty()) {
            int revoked = sharedItemMemberRepository.revokeMembers(
                    sharedItemId,
                    new ArrayList<>(revokedMemberIds),
                    SharedItemMemberStatus.ACTIVE,
                    SharedItemMemberStatus.REVOKED,
                    now
            );
            if (revoked != revokedMemberIds.size()) {
                throw conflict("SHARED_ITEM_MEMBER_REVOKE_CONFLICT", "Shared item member revoke conflicted");
            }
        }
    }

    private RotationMemberResponse toRotationMemberResponse(SharedItemMember member) {
        MemberShareKey shareKey = findActiveShareKey(member.getMember().getMemberId());
        return new RotationMemberResponse(
                member.getMember().getMemberId(),
                member.getMemberRole().name(),
                member.getPermission().name(),
                member.getStatus().name(),
                shareKey.getPublicKeyBase64(),
                shareKey.getKeyVersion()
        );
    }

    private SharedItem findSharedItem(Long sharedItemId) {
        return sharedItemRepository.findById(sharedItemId)
                .orElseThrow(() -> notFound("SHARED_ITEM_NOT_FOUND", "Shared item not found"));
    }

    private MemberShareKey findActiveShareKey(Long memberId) {
        return memberShareKeyRepository
                .findTopByMemberMemberIdAndStatusOrderByKeyVersionDesc(memberId, ShareKeyStatus.ACTIVE)
                .orElseThrow(() -> notFound("MEMBER_SHARE_KEY_NOT_FOUND", "Active member share key not found"));
    }

    private SharedItemMember requireActiveMembership(List<SharedItemMember> members, Long requesterId) {
        return members.stream()
                .filter(member -> member.getMember().getMemberId().equals(requesterId))
                .filter(member -> member.getStatus() == SharedItemMemberStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> forbidden("SHARED_ITEM_ACCESS_DENIED", "Shared item access denied"));
    }

    private void requireActiveSharedItem(SharedItem sharedItem) {
        if (sharedItem.getStatus() != SharedItemStatus.ACTIVE) {
            throw badRequest("SHARED_ITEM_NOT_ACTIVE", "Shared item is not active");
        }
    }

    private void requireOwner(SharedItemMember member) {
        if (member.getMemberRole() != SharedItemMemberRole.OWNER) {
            throw forbidden("ONLY_OWNER_CAN_ROTATE_KEY", "Only owner can rotate shared item key");
        }
    }

    private void validateExpectedVersion(SharedItem sharedItem, Long expectedVersion, Long expectedKeyVersion) {
        if (!sharedItem.getVersion().equals(expectedVersion)) {
            throw conflict("SHARED_ITEM_VERSION_CONFLICT", "Shared item version conflict");
        }
        if (!sharedItem.getKeyVersion().equals(expectedKeyVersion)) {
            throw conflict("SHARED_ITEM_KEY_VERSION_CONFLICT", "Shared item keyVersion conflict");
        }
    }

    private Set<Long> normalizeRevokedMemberIds(List<Long> revokedMemberIds) {
        Set<Long> normalized = new LinkedHashSet<>();
        if (revokedMemberIds == null) {
            return normalized;
        }
        for (Long memberId : revokedMemberIds) {
            if (memberId == null) {
                throw badRequest("INVALID_REVOKED_MEMBER", "revokedMemberIds must not contain null");
            }
            normalized.add(memberId);
        }
        return normalized;
    }

    private void validateRevokedMembers(List<SharedItemMember> members, Set<Long> revokedMemberIds) {
        Map<Long, SharedItemMember> membersById = new HashMap<>();
        for (SharedItemMember member : members) {
            membersById.put(member.getMember().getMemberId(), member);
        }

        for (Long revokedMemberId : revokedMemberIds) {
            SharedItemMember member = membersById.get(revokedMemberId);
            if (member == null) {
                throw badRequest("REVOKED_MEMBER_NOT_FOUND", "Revoked member does not belong to shared item");
            }
            if (member.getMemberRole() == SharedItemMemberRole.OWNER) {
                throw badRequest("CANNOT_REVOKE_OWNER", "Owner cannot be revoked");
            }
            if (member.getStatus() == SharedItemMemberStatus.REVOKED) {
                throw badRequest("SHARED_ITEM_MEMBER_ALREADY_REVOKED", "Revoked member is already revoked");
            }
        }
    }

    private void validateRemainingActiveMembers(List<SharedItemMember> remainingActiveMembers) {
        if (remainingActiveMembers.isEmpty()) {
            throw badRequest("NO_REMAINING_ACTIVE_MEMBER", "At least one active member must remain");
        }
        boolean ownerRemains = remainingActiveMembers.stream()
                .anyMatch(member -> member.getMemberRole() == SharedItemMemberRole.OWNER);
        if (!ownerRemains) {
            throw badRequest("CANNOT_REVOKE_OWNER", "Owner must remain active");
        }
    }

    private Map<Long, MemberKeyWrapperRequest> validateWrapperCompleteness(
            List<SharedItemMember> remainingActiveMembers,
            List<MemberKeyWrapperRequest> wrappers,
            Set<Long> revokedMemberIds
    ) {
        if (wrappers == null || wrappers.isEmpty()) {
            throw badRequest("MISSING_MEMBER_KEY_WRAPPER", "Member key wrappers are required");
        }

        Map<Long, MemberKeyWrapperRequest> wrappersByMemberId = new HashMap<>();
        Set<Long> wrapperMemberIds = new HashSet<>();
        for (MemberKeyWrapperRequest wrapper : wrappers) {
            if (wrapper == null || wrapper.memberId() == null) {
                throw badRequest("MISSING_MEMBER_KEY_WRAPPER", "Member key wrapper memberId is required");
            }
            if (!wrapperMemberIds.add(wrapper.memberId())) {
                throw badRequest("DUPLICATE_MEMBER_KEY_WRAPPER", "Duplicate member key wrapper");
            }
            if (revokedMemberIds.contains(wrapper.memberId())) {
                throw badRequest("REVOKED_MEMBER_WRAPPER_NOT_ALLOWED", "Revoked member wrapper is not allowed");
            }
            wrappersByMemberId.put(wrapper.memberId(), wrapper);
        }

        Set<Long> remainingMemberIds = new HashSet<>();
        for (SharedItemMember member : remainingActiveMembers) {
            remainingMemberIds.add(member.getMember().getMemberId());
        }

        if (wrappers.size() != remainingActiveMembers.size() || !wrapperMemberIds.equals(remainingMemberIds)) {
            throw badRequest("MISSING_MEMBER_KEY_WRAPPER", "Wrappers for every remaining active member are required");
        }

        return wrappersByMemberId;
    }

    private void validateRecipientKeyVersions(
            List<SharedItemMember> remainingActiveMembers,
            Map<Long, MemberKeyWrapperRequest> wrappersByMemberId
    ) {
        for (SharedItemMember member : remainingActiveMembers) {
            Long memberId = member.getMember().getMemberId();
            MemberShareKey shareKey = findActiveShareKey(memberId);
            MemberKeyWrapperRequest wrapper = wrappersByMemberId.get(memberId);
            if (!shareKey.getKeyVersion().equals(wrapper.recipientKeyVersion())) {
                throw conflict("RECIPIENT_KEY_VERSION_CONFLICT", "Recipient key version conflict");
            }
        }
    }

    private void validateRotateRequest(RotateSharedItemKeyRequest request) {
        if (request == null) {
            throw badRequest("INVALID_CIPHER_PAYLOAD", "Request body is required");
        }
        if (request.expectedVersion() == null || request.expectedKeyVersion() == null) {
            throw badRequest("INVALID_CIPHER_PAYLOAD", "expectedVersion and expectedKeyVersion are required");
        }
        validateCipherText(request.titleCipherBase64());
        validateCipherText(request.itemCipherBase64());
        if (request.memberKeyWrappers() == null || request.memberKeyWrappers().isEmpty()) {
            throw badRequest("MISSING_MEMBER_KEY_WRAPPER", "Member key wrappers are required");
        }
        for (MemberKeyWrapperRequest wrapper : request.memberKeyWrappers()) {
            if (wrapper == null || wrapper.memberId() == null || wrapper.recipientKeyVersion() == null) {
                throw badRequest("MISSING_MEMBER_KEY_WRAPPER", "Member key wrapper fields are required");
            }
            validateCipherText(wrapper.encryptedItemKeyBase64());
        }
    }

    private void validateCipherText(String value) {
        if (value == null || value.isBlank()) {
            throw badRequest("INVALID_CIPHER_PAYLOAD", "Cipher payload is required");
        }
        if (value.length() > MAX_CIPHER_TEXT_LENGTH) {
            throw badRequest("INVALID_CIPHER_PAYLOAD", "Cipher payload is too long");
        }
        try {
            Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw badRequest("INVALID_CIPHER_PAYLOAD", "Cipher payload must be valid Base64");
        }
    }

    private SharedItemException badRequest(String code, String message) {
        return new SharedItemException(HttpStatus.BAD_REQUEST, code, message);
    }

    private SharedItemException forbidden(String code, String message) {
        return new SharedItemException(HttpStatus.FORBIDDEN, code, message);
    }

    private SharedItemException notFound(String code, String message) {
        return new SharedItemException(HttpStatus.NOT_FOUND, code, message);
    }

    private SharedItemException conflict(String code, String message) {
        return new SharedItemException(HttpStatus.CONFLICT, code, message);
    }
}
