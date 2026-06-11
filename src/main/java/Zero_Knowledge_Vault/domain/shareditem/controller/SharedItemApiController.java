package Zero_Knowledge_Vault.domain.shareditem.controller;

import Zero_Knowledge_Vault.domain.shareditem.dto.request.ApproveJoinRequestRequest;
import Zero_Knowledge_Vault.domain.shareditem.dto.request.CreateSharedItemRequest;
import Zero_Knowledge_Vault.domain.shareditem.dto.request.UpdateSharedItemMemberPermissionRequest;
import Zero_Knowledge_Vault.domain.shareditem.dto.request.UpdateSharedItemRequest;
import Zero_Knowledge_Vault.domain.shareditem.dto.response.*;
import Zero_Knowledge_Vault.domain.shareditem.service.SharedItemService;
import Zero_Knowledge_Vault.infra.swagger.OpenApiConfig;
import Zero_Knowledge_Vault.infra.security.jwt.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shared-items")
@RequiredArgsConstructor
@Tag(
        name = "Shared Item",
        description = "Client-encrypted shared item, invite, join request, and member management APIs"
)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class SharedItemApiController {

    private final SharedItemService sharedItemService;

    @PostMapping
    @Operation(
            summary = "Create a shared item",
            description = """
                    Creates a shared secret owned by the caller. The client encrypts title/content with sharedItemKey before sending.
                    The server stores only titleCipherBase64, itemCipherBase64, and ownerEncryptedItemKeyBase64.
                    The server never receives sharedItemKey plaintext or shared secret plaintext.
                    """
    )
    public ResponseEntity<SharedItemResponse> createSharedItem(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreateSharedItemRequest request
    ) {
        return ResponseEntity.ok(sharedItemService.createSharedItem(principal.getUserId(), request));
    }

    @GetMapping
    @Operation(
            summary = "List shared items accessible by the caller",
            description = "Returns owner and participant shared items that the caller can access. encryptedItemKeyBase64 is scoped to the current member."
    )
    public ResponseEntity<List<SharedItemResponse>> getSharedItems(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(sharedItemService.getSharedItems(principal.getUserId()));
    }

    @GetMapping("/{sharedItemId}")
    @Operation(
            summary = "Get a shared item accessible by the caller",
            description = "Returns encrypted title/content and the current member's encryptedItemKeyBase64. Client decrypts locally."
    )
    public ResponseEntity<SharedItemResponse> getSharedItem(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "Shared item id", example = "1")
            @PathVariable Long sharedItemId
    ) {
        return ResponseEntity.ok(sharedItemService.getSharedItem(principal.getUserId(), sharedItemId));
    }

    @PatchMapping("/{sharedItemId}")
    @Operation(
            summary = "Update shared item ciphertext",
            description = """
                    Updates encrypted shared item title/content only. Requires OWNER or PARTICIPANT with READ_WRITE permission.
                    expectedVersion is used for optimistic concurrency control so older ciphertext cannot silently overwrite newer ciphertext.
                    """
    )
    public ResponseEntity<UpdateSharedItemResponse> updateSharedItem(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "Shared item id", example = "1")
            @PathVariable Long sharedItemId,
            @Valid @RequestBody UpdateSharedItemRequest request
    ) {
        return ResponseEntity.ok(sharedItemService.updateSharedItem(principal.getUserId(), sharedItemId, request));
    }

    @DeleteMapping("/{sharedItemId}")
    @Operation(
            summary = "Soft-delete a shared item",
            description = "OWNER only. Marks the shared item deleted and prevents further normal access."
    )
    public ResponseEntity<DeleteSharedItemResponse> deleteSharedItem(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "Shared item id", example = "1")
            @PathVariable Long sharedItemId
    ) {
        return ResponseEntity.ok(sharedItemService.deleteSharedItem(principal.getUserId(), sharedItemId));
    }

    @PostMapping("/{sharedItemId}/invite-links")
    @Operation(
            summary = "Create a short-lived invite link. OWNER only.",
            description = "OWNER only. Returns the raw invite token once in inviteUrl. Only inviteTokenHash is stored server-side."
    )
    public ResponseEntity<CreateInviteLinkResponse> createInviteLink(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "Shared item id to invite a participant into", example = "1")
            @PathVariable Long sharedItemId
    ) {
        return ResponseEntity.ok(sharedItemService.createInviteLink(principal.getUserId(), sharedItemId));
    }

    @PostMapping("/invite-links/{token}/join-requests")
    @Operation(
            summary = "Create a join request from an invite token",
            description = """
                    Creates a pending request only. This does not grant access and does not receive sharedItemKey plaintext.
                    The owner must later approve the request using the requester's public key.
                    """
    )
    public ResponseEntity<CreateJoinRequestResponse> createJoinRequest(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "Raw invite token from the invite URL", example = "invite-token-value")
            @PathVariable String token
    ) {
        return ResponseEntity.ok(sharedItemService.createJoinRequest(principal.getUserId(), token));
    }

    @GetMapping("/{sharedItemId}/join-requests")
    @Operation(
            summary = "List pending join requests for a shared item",
            description = "OWNER only. Returns requester public share keys so the owner client can wrap sharedItemKey for each requester."
    )
    public ResponseEntity<List<PendingJoinRequestResponse>> getPendingJoinRequests(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "Shared item id", example = "1")
            @PathVariable Long sharedItemId
    ) {
        return ResponseEntity.ok(sharedItemService.getPendingJoinRequests(principal.getUserId(), sharedItemId));
    }

    @PostMapping("/{sharedItemId}/join-requests/{joinRequestId}/approve")
    @Operation(
            summary = "Approve a pending join request. OWNER only.",
            description = """
                    OWNER only. Owner client encrypts sharedItemKey with requesterPublicKeyBase64 and sends encryptedItemKeyBase64.
                    The server stores that wrapped key for the requester and marks the request APPROVED if still pending.
                    """
    )
    public ResponseEntity<JoinRequestDecisionResponse> approveJoinRequest(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "Shared item id", example = "1")
            @PathVariable Long sharedItemId,
            @Parameter(description = "Pending join request id", example = "10")
            @PathVariable Long joinRequestId,
            @Valid @RequestBody ApproveJoinRequestRequest request
    ) {
        return ResponseEntity.ok(sharedItemService.approveJoinRequest(
                principal.getUserId(),
                sharedItemId,
                joinRequestId,
                request
        ));
    }

    @PostMapping("/{sharedItemId}/join-requests/{joinRequestId}/reject")
    @Operation(
            summary = "Reject a pending join request",
            description = "OWNER only. Rejects a still-pending join request without sharing encryptedItemKeyBase64."
    )
    public ResponseEntity<JoinRequestDecisionResponse> rejectJoinRequest(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "Shared item id", example = "1")
            @PathVariable Long sharedItemId,
            @Parameter(description = "Pending join request id", example = "10")
            @PathVariable Long joinRequestId
    ) {
        return ResponseEntity.ok(sharedItemService.rejectJoinRequest(
                principal.getUserId(),
                sharedItemId,
                joinRequestId
        ));
    }

    @GetMapping("/{sharedItemId}/members")
    @Operation(
            summary = "List active shared item members. OWNER only.",
            description = "Does not include encryptedItemKeyBase64 or private key material."
    )
    public ResponseEntity<List<SharedItemMemberResponse>> getMembers(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "Shared item id", example = "1")
            @PathVariable Long sharedItemId
    ) {
        return ResponseEntity.ok(sharedItemService.getMembers(principal.getUserId(), sharedItemId));
    }

    @PatchMapping("/{sharedItemId}/members/{memberId}/permission")
    @Operation(
            summary = "Update an active participant permission",
            description = "OWNER only. Changes READ_ONLY/READ_WRITE permission metadata; it does not re-encrypt content or expose sharedItemKey."
    )
    public ResponseEntity<UpdateSharedItemMemberPermissionResponse> updateMemberPermission(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "Shared item id", example = "1")
            @PathVariable Long sharedItemId,
            @Parameter(description = "Participant member id", example = "2")
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateSharedItemMemberPermissionRequest request
    ) {
        return ResponseEntity.ok(sharedItemService.updateMemberPermission(
                principal.getUserId(),
                sharedItemId,
                memberId,
                request
        ));
    }

    @PostMapping("/{sharedItemId}/members/{memberId}/revoke")
    @Operation(
            summary = "Revoke an active participant",
            description = "OWNER only. Marks the participant member record revoked. Existing cryptographic rotation is not performed by this endpoint."
    )
    public ResponseEntity<RevokeSharedItemMemberResponse> revokeMember(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "Shared item id", example = "1")
            @PathVariable Long sharedItemId,
            @Parameter(description = "Participant member id", example = "2")
            @PathVariable Long memberId
    ) {
        return ResponseEntity.ok(sharedItemService.revokeMember(principal.getUserId(), sharedItemId, memberId));
    }
}
