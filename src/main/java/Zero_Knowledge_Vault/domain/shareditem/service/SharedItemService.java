package Zero_Knowledge_Vault.domain.shareditem.service;

import Zero_Knowledge_Vault.domain.member.entity.Member;
import Zero_Knowledge_Vault.domain.member.repository.MemberRepository;
import Zero_Knowledge_Vault.domain.share.entity.MemberShareKey;
import Zero_Knowledge_Vault.domain.share.repository.MemberShareKeyRepository;
import Zero_Knowledge_Vault.domain.share.type.ShareKeyStatus;
import Zero_Knowledge_Vault.domain.shareditem.dto.request.ApproveJoinRequestRequest;
import Zero_Knowledge_Vault.domain.shareditem.dto.request.CreateSharedItemRequest;
import Zero_Knowledge_Vault.domain.shareditem.dto.request.UpdateSharedItemMemberPermissionRequest;
import Zero_Knowledge_Vault.domain.shareditem.dto.request.UpdateSharedItemRequest;
import Zero_Knowledge_Vault.domain.shareditem.dto.response.*;
import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItem;
import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemInviteLink;
import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemJoinRequest;
import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemMember;
import Zero_Knowledge_Vault.global.exception.custom.SharedItemException;
import Zero_Knowledge_Vault.domain.shareditem.repository.SharedItemInviteLinkRepository;
import Zero_Knowledge_Vault.domain.shareditem.repository.SharedItemJoinRequestRepository;
import Zero_Knowledge_Vault.domain.shareditem.repository.SharedItemMemberRepository;
import Zero_Knowledge_Vault.domain.shareditem.repository.SharedItemRepository;
import Zero_Knowledge_Vault.domain.shareditem.type.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SharedItemService {

    private static final String DEFAULT_ITEM_CIPHER_ALGORITHM = "AES-GCM-256";
    private static final int INVITE_TOKEN_BYTES = 32;
    private static final int INVITE_TTL_MINUTES = 3;
    private static final int MAX_CIPHER_TEXT_LENGTH = 1_000_000;

    private final SecureRandom secureRandom = new SecureRandom();

    private final MemberRepository memberRepository;
    private final MemberShareKeyRepository memberShareKeyRepository;
    private final SharedItemRepository sharedItemRepository;
    private final SharedItemMemberRepository sharedItemMemberRepository;
    private final SharedItemInviteLinkRepository sharedItemInviteLinkRepository;
    private final SharedItemJoinRequestRepository sharedItemJoinRequestRepository;

    @Value("${domain.name:http://localhost:8080}")
    private String domainName;

    @Transactional
    public SharedItemResponse createSharedItem(Long memberId, CreateSharedItemRequest request) {
        validateCreateRequest(request);
        Member owner = findMember(memberId);
        MemberShareKey ownerShareKey = findActiveShareKey(memberId, "OWNER_SHARE_KEY_NOT_FOUND");

        if (!ownerShareKey.getKeyVersion().equals(request.ownerKeyVersion())) {
            throw badRequest("OWNER_KEY_VERSION_MISMATCH", "ownerKeyVersion does not match active share key");
        }

        LocalDateTime now = LocalDateTime.now();
        SharedItem item = SharedItem.create(
                owner,
                request.titleCipherBase64(),
                request.itemCipherBase64(),
                request.itemCipherAlgorithm()
        );

        try {
            sharedItemRepository.saveAndFlush(item);
            SharedItemMember ownerMember = SharedItemMember.owner(
                    item,
                    owner,
                    request.ownerKeyVersion(),
                    request.ownerEncryptedItemKeyBase64(),
                    now
            );
            sharedItemMemberRepository.saveAndFlush(ownerMember);
            return SharedItemResponse.from(ownerMember);
        } catch (DataIntegrityViolationException e) {
            throw conflict("SHARED_ITEM_CREATE_CONFLICT", "Shared item creation conflicted");
        }
    }

    public List<SharedItemResponse> getSharedItems(Long memberId) {
        return sharedItemMemberRepository
                .findAccessibleItems(
                        memberId,
                        SharedItemMemberStatus.ACTIVE,
                        SharedItemStatus.ACTIVE
                )
                .stream()
                .map(SharedItemResponse::from)
                .toList();
    }

    public SharedItemResponse getSharedItem(Long memberId, Long sharedItemId) {
        SharedItemMember member = requireActiveMember(sharedItemId, memberId);
        requireActiveItem(member.getSharedItem());
        return SharedItemResponse.from(member);
    }

    @Transactional
    public UpdateSharedItemResponse updateSharedItem(Long memberId, Long sharedItemId, UpdateSharedItemRequest request) {
        validateUpdateRequest(request);
        SharedItemMember member = requireActiveMember(sharedItemId, memberId);
        requireActiveItem(member.getSharedItem());
        requireReadWrite(member);

        LocalDateTime now = LocalDateTime.now();
        int updated = sharedItemRepository.updateCipherIfVersionMatches(
                sharedItemId,
                request.expectedVersion(),
                request.titleCipherBase64(),
                request.itemCipherBase64(),
                now,
                SharedItemStatus.ACTIVE
        );

        if (updated == 0) {
            throw conflict("SHARED_ITEM_VERSION_CONFLICT", "Shared item version conflict");
        }

        SharedItem item = findSharedItem(sharedItemId);
        return new UpdateSharedItemResponse(sharedItemId, item.getVersion(), item.getUpdatedAt());
    }

    @Transactional
    public DeleteSharedItemResponse deleteSharedItem(Long memberId, Long sharedItemId) {
        SharedItemMember member = requireActiveMember(sharedItemId, memberId);
        SharedItem item = member.getSharedItem();
        requireActiveItem(item);
        requireOwner(member);

        item.markDeleted(LocalDateTime.now());
        return new DeleteSharedItemResponse(true);
    }

    @Transactional
    public CreateInviteLinkResponse createInviteLink(Long memberId, Long sharedItemId) {
        SharedItemMember member = requireActiveMember(sharedItemId, memberId);
        SharedItem item = member.getSharedItem();
        requireActiveItem(item);
        requireOwner(member);

        String token = generateInviteToken();
        String tokenHash = hashInviteToken(token);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(INVITE_TTL_MINUTES);
        SharedItemInviteLink link = SharedItemInviteLink.create(item, member.getMember(), tokenHash, expiresAt);

        try {
            sharedItemInviteLinkRepository.saveAndFlush(link);
        } catch (DataIntegrityViolationException e) {
            throw conflict("INVITE_TOKEN_CONFLICT", "Invite token creation conflicted");
        }

        return CreateInviteLinkResponse.from(link, buildInviteUrl(token));
    }

    @Transactional
    public CreateJoinRequestResponse createJoinRequest(Long requesterMemberId, String token) {
        if (token == null || token.isBlank()) {
            throw badRequest("INVITE_TOKEN_REQUIRED", "Invite token is required");
        }

        LocalDateTime now = LocalDateTime.now();
        SharedItemInviteLink link = sharedItemInviteLinkRepository
                .findByInviteTokenHashAndStatusAndExpiresAtAfter(
                        hashInviteToken(token),
                        SharedItemInviteLinkStatus.ACTIVE,
                        now
                )
                .orElseThrow(() -> notFound("INVITE_LINK_NOT_FOUND", "Invite link not found or expired"));

        SharedItem item = link.getSharedItem();
        requireActiveItem(item);

        if (item.getOwner().getMemberId().equals(requesterMemberId)) {
            throw forbidden("OWNER_CANNOT_REQUEST_JOIN", "Owner cannot request to join own shared item");
        }

        if (sharedItemMemberRepository.existsBySharedItemSharedItemIdAndMemberMemberId(
                item.getSharedItemId(),
                requesterMemberId
        )) {
            throw conflict("SHARED_ITEM_MEMBER_ALREADY_EXISTS", "Requester is already a shared item member");
        }

        Member requester = findMember(requesterMemberId);
        MemberShareKey requesterShareKey = findActiveShareKey(requesterMemberId, "REQUESTER_SHARE_KEY_NOT_FOUND");

        SharedItemJoinRequest request = SharedItemJoinRequest.create(
                item,
                link,
                requester,
                requesterShareKey.getKeyVersion()
        );

        try {
            return CreateJoinRequestResponse.from(sharedItemJoinRequestRepository.saveAndFlush(request));
        } catch (DataIntegrityViolationException e) {
            throw conflict("JOIN_REQUEST_ALREADY_EXISTS", "Join request already exists");
        }
    }

    public List<PendingJoinRequestResponse> getPendingJoinRequests(Long memberId, Long sharedItemId) {
        SharedItemMember owner = requireActiveMember(sharedItemId, memberId);
        requireActiveItem(owner.getSharedItem());
        requireOwner(owner);

        return sharedItemJoinRequestRepository
                .findBySharedItemSharedItemIdAndStatusOrderByRequestedAtAsc(
                        sharedItemId,
                        SharedItemJoinRequestStatus.PENDING
                )
                .stream()
                .map(request -> {
                    MemberShareKey requesterKey = findActiveShareKey(
                            request.getRequester().getMemberId(),
                            "REQUESTER_SHARE_KEY_NOT_FOUND"
                    );
                    return PendingJoinRequestResponse.from(
                            request,
                            requesterKey.getPublicKeyBase64(),
                            maskEmail(request.getRequester().getEmail())
                    );
                })
                .toList();
    }

    @Transactional
    public JoinRequestDecisionResponse approveJoinRequest(
            Long ownerMemberId,
            Long sharedItemId,
            Long joinRequestId,
            ApproveJoinRequestRequest request
    ) {
        validateApproveRequest(request);
        SharedItemMember owner = requireActiveMember(sharedItemId, ownerMemberId);
        requireActiveItem(owner.getSharedItem());
        requireOwner(owner);

        SharedItemJoinRequest joinRequest = findJoinRequest(joinRequestId);
        validateJoinRequestForItem(joinRequest, sharedItemId);
        validateJoinRequestPending(joinRequest);

        Long requesterMemberId = joinRequest.getRequester().getMemberId();
        if (sharedItemMemberRepository.existsBySharedItemSharedItemIdAndMemberMemberId(sharedItemId, requesterMemberId)) {
            throw conflict("SHARED_ITEM_MEMBER_ALREADY_EXISTS", "Requester is already a shared item member");
        }

        MemberShareKey requesterShareKey = findActiveShareKey(requesterMemberId, "REQUESTER_SHARE_KEY_NOT_FOUND");
        if (!requesterShareKey.getKeyVersion().equals(request.recipientKeyVersion())) {
            throw badRequest("RECIPIENT_KEY_VERSION_MISMATCH", "recipientKeyVersion does not match requester active share key");
        }

        LocalDateTime now = LocalDateTime.now();
        SharedItemMember participant = SharedItemMember.participant(
                joinRequest.getSharedItem(),
                joinRequest.getRequester(),
                request.normalizedPermission(),
                request.recipientKeyVersion(),
                request.encryptedItemKeyBase64(),
                now
        );

        try {
            sharedItemMemberRepository.saveAndFlush(participant);
            int decided = sharedItemJoinRequestRepository.decideIfPending(
                    joinRequestId,
                    sharedItemId,
                    SharedItemJoinRequestStatus.PENDING,
                    SharedItemJoinRequestStatus.APPROVED,
                    now,
                    owner.getMember()
            );

            if (decided == 0) {
                throw conflict("JOIN_REQUEST_ALREADY_DECIDED", "Join request has already been decided");
            }
        } catch (DataIntegrityViolationException e) {
            throw conflict("SHARED_ITEM_MEMBER_ALREADY_EXISTS", "Requester is already a shared item member");
        }

        return new JoinRequestDecisionResponse(
                joinRequestId,
                sharedItemId,
                SharedItemJoinRequestStatus.APPROVED,
                now
        );
    }

    @Transactional
    public JoinRequestDecisionResponse rejectJoinRequest(Long ownerMemberId, Long sharedItemId, Long joinRequestId) {
        SharedItemMember owner = requireActiveMember(sharedItemId, ownerMemberId);
        requireActiveItem(owner.getSharedItem());
        requireOwner(owner);

        SharedItemJoinRequest joinRequest = findJoinRequest(joinRequestId);
        validateJoinRequestForItem(joinRequest, sharedItemId);
        validateJoinRequestPending(joinRequest);

        LocalDateTime now = LocalDateTime.now();
        int decided = sharedItemJoinRequestRepository.decideIfPending(
                joinRequestId,
                sharedItemId,
                SharedItemJoinRequestStatus.PENDING,
                SharedItemJoinRequestStatus.REJECTED,
                now,
                owner.getMember()
        );

        if (decided == 0) {
            throw conflict("JOIN_REQUEST_ALREADY_DECIDED", "Join request has already been decided");
        }

        return new JoinRequestDecisionResponse(
                joinRequestId,
                sharedItemId,
                SharedItemJoinRequestStatus.REJECTED,
                now
        );
    }

    public List<SharedItemMemberResponse> getMembers(Long ownerMemberId, Long sharedItemId) {
        SharedItemMember owner = requireActiveMember(sharedItemId, ownerMemberId);
        requireActiveItem(owner.getSharedItem());
        requireOwner(owner);

        return sharedItemMemberRepository
                .findBySharedItemSharedItemIdAndStatusOrderByCreatedAtAsc(
                        sharedItemId,
                        SharedItemMemberStatus.ACTIVE
                )
                .stream()
                .map(member -> SharedItemMemberResponse.from(member, maskEmail(member.getMember().getEmail())))
                .toList();
    }

    @Transactional
    public UpdateSharedItemMemberPermissionResponse updateMemberPermission(
            Long ownerMemberId,
            Long sharedItemId,
            Long targetMemberId,
            UpdateSharedItemMemberPermissionRequest request
    ) {
        SharedItemMember owner = requireActiveMember(sharedItemId, ownerMemberId);
        requireActiveItem(owner.getSharedItem());
        requireOwner(owner);

        if (ownerMemberId.equals(targetMemberId)) {
            throw forbidden("OWNER_PERMISSION_CHANGE_FORBIDDEN", "Owner permission cannot be changed");
        }

        SharedItemMember target = requireActiveMember(sharedItemId, targetMemberId);
        requireParticipant(target);
        if (request.permission() == null) {
            throw badRequest("INVALID_PERMISSION", "Permission must be READ_ONLY or READ_WRITE");
        }

        target.updatePermission(request.permission());
        return UpdateSharedItemMemberPermissionResponse.from(target, LocalDateTime.now());
    }

    @Transactional
    public RevokeSharedItemMemberResponse revokeMember(Long ownerMemberId, Long sharedItemId, Long targetMemberId) {
        SharedItemMember owner = requireActiveMember(sharedItemId, ownerMemberId);
        requireActiveItem(owner.getSharedItem());
        requireOwner(owner);

        if (ownerMemberId.equals(targetMemberId)) {
            throw forbidden("OWNER_REVOKE_FORBIDDEN", "Owner cannot be revoked");
        }

        SharedItemMember target = requireActiveMember(sharedItemId, targetMemberId);
        requireParticipant(target);
        target.revoke(LocalDateTime.now());

        return new RevokeSharedItemMemberResponse(sharedItemId, targetMemberId, true);
    }

    private SharedItem findSharedItem(Long sharedItemId) {
        return sharedItemRepository.findById(sharedItemId)
                .orElseThrow(() -> notFound("SHARED_ITEM_NOT_FOUND", "Shared item not found"));
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> notFound("MEMBER_NOT_FOUND", "Member not found"));
    }

    private MemberShareKey findActiveShareKey(Long memberId, String code) {
        return memberShareKeyRepository
                .findTopByMemberMemberIdAndStatusOrderByKeyVersionDesc(memberId, ShareKeyStatus.ACTIVE)
                .orElseThrow(() -> notFound(code, "Active share key not found"));
    }

    private SharedItemMember requireActiveMember(Long sharedItemId, Long memberId) {
        SharedItemMember member = sharedItemMemberRepository
                .findBySharedItemSharedItemIdAndMemberMemberId(sharedItemId, memberId)
                .orElseThrow(() -> forbidden("SHARED_ITEM_ACCESS_DENIED", "Shared item access denied"));

        requireActiveItem(member.getSharedItem());
        if (member.getStatus() != SharedItemMemberStatus.ACTIVE) {
            throw forbidden("SHARED_ITEM_MEMBER_REVOKED", "Shared item member is revoked");
        }

        return member;
    }

    private void requireActiveItem(SharedItem item) {
        if (item.getStatus() != SharedItemStatus.ACTIVE) {
            throw notFound("SHARED_ITEM_NOT_FOUND", "Shared item not found");
        }
    }

    private void requireOwner(SharedItemMember member) {
        if (member.getMemberRole() != SharedItemMemberRole.OWNER) {
            throw forbidden("OWNER_REQUIRED", "Owner permission is required");
        }
    }

    private void requireParticipant(SharedItemMember member) {
        if (member.getMemberRole() != SharedItemMemberRole.PARTICIPANT) {
            throw forbidden("PARTICIPANT_REQUIRED", "Target member must be an active participant");
        }
    }

    private void requireReadWrite(SharedItemMember member) {
        if (member.getPermission() != SharedItemPermission.READ_WRITE) {
            throw forbidden("READ_WRITE_REQUIRED", "READ_WRITE permission is required");
        }
    }

    private SharedItemJoinRequest findJoinRequest(Long joinRequestId) {
        return sharedItemJoinRequestRepository.findById(joinRequestId)
                .orElseThrow(() -> notFound("JOIN_REQUEST_NOT_FOUND", "Join request not found"));
    }

    private void validateJoinRequestForItem(SharedItemJoinRequest request, Long sharedItemId) {
        if (!request.getSharedItem().getSharedItemId().equals(sharedItemId)) {
            throw notFound("JOIN_REQUEST_NOT_FOUND", "Join request not found");
        }
    }

    private void validateJoinRequestPending(SharedItemJoinRequest request) {
        if (request.getStatus() != SharedItemJoinRequestStatus.PENDING) {
            throw conflict("JOIN_REQUEST_ALREADY_DECIDED", "Join request has already been decided");
        }
    }

    private void validateCreateRequest(CreateSharedItemRequest request) {
        validateCipherText("titleCipherBase64", request.titleCipherBase64(), false);
        validateCipherText("itemCipherBase64", request.itemCipherBase64(), true);
        validateCipherText("ownerEncryptedItemKeyBase64", request.ownerEncryptedItemKeyBase64(), true);
        validateAlgorithm(request.itemCipherAlgorithm());
    }

    private void validateUpdateRequest(UpdateSharedItemRequest request) {
        validateCipherText("titleCipherBase64", request.titleCipherBase64(), false);
        validateCipherText("itemCipherBase64", request.itemCipherBase64(), true);
    }

    private void validateApproveRequest(ApproveJoinRequestRequest request) {
        validateCipherText("encryptedItemKeyBase64", request.encryptedItemKeyBase64(), true);
        if (request.normalizedPermission() == null) {
            throw badRequest("INVALID_PERMISSION", "Permission must be READ_ONLY or READ_WRITE");
        }
    }

    private void validateAlgorithm(String algorithm) {
        if (!DEFAULT_ITEM_CIPHER_ALGORITHM.equals(algorithm)) {
            throw badRequest("UNSUPPORTED_SHARED_ITEM_ALGORITHM", "Unsupported shared item algorithm");
        }
    }

    private void validateCipherText(String fieldName, String value, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) {
                throw badRequest("SHARED_ITEM_FIELD_REQUIRED", fieldName + " is required");
            }
            return;
        }

        if (value.length() > MAX_CIPHER_TEXT_LENGTH) {
            throw badRequest("SHARED_ITEM_FIELD_TOO_LONG", fieldName + " is too long");
        }

        try {
            Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw badRequest("INVALID_SHARED_ITEM_BASE64", fieldName + " must be valid Base64");
        }
    }

    private String generateInviteToken() {
        byte[] bytes = new byte[INVITE_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashInviteToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String buildInviteUrl(String token) {
        return domainName + "/shared-invite.html?token=" + token;
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }

        int at = email.indexOf("@");
        if (at <= 1) {
            return "***" + (at >= 0 ? email.substring(at) : "");
        }

        return email.substring(0, Math.min(3, at)) + "***" + email.substring(at);
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
