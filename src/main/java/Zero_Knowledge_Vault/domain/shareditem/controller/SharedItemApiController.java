package Zero_Knowledge_Vault.domain.shareditem.controller;

import Zero_Knowledge_Vault.domain.shareditem.dto.request.ApproveJoinRequestRequest;
import Zero_Knowledge_Vault.domain.shareditem.dto.request.CreateSharedItemRequest;
import Zero_Knowledge_Vault.domain.shareditem.dto.request.UpdateSharedItemMemberPermissionRequest;
import Zero_Knowledge_Vault.domain.shareditem.dto.request.UpdateSharedItemRequest;
import Zero_Knowledge_Vault.domain.shareditem.dto.response.*;
import Zero_Knowledge_Vault.domain.shareditem.service.SharedItemService;
import Zero_Knowledge_Vault.infra.security.jwt.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Shared Item", description = "Client-encrypted shared item and invitation APIs")
public class SharedItemApiController {

    private final SharedItemService sharedItemService;

    @PostMapping
    @Operation(
            summary = "Create a shared item",
            description = "Stores only client-encrypted shared item ciphertext and the owner encrypted item key. The server never receives sharedItemKey or plaintext title/content."
    )
    public ResponseEntity<SharedItemResponse> createSharedItem(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreateSharedItemRequest request
    ) {
        return ResponseEntity.ok(sharedItemService.createSharedItem(principal.getUserId(), request));
    }

    @GetMapping
    @Operation(summary = "List shared items accessible by the caller")
    public ResponseEntity<List<SharedItemResponse>> getSharedItems(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(sharedItemService.getSharedItems(principal.getUserId()));
    }

    @GetMapping("/{sharedItemId}")
    @Operation(summary = "Get a shared item accessible by the caller")
    public ResponseEntity<SharedItemResponse> getSharedItem(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long sharedItemId
    ) {
        return ResponseEntity.ok(sharedItemService.getSharedItem(principal.getUserId(), sharedItemId));
    }

    @PatchMapping("/{sharedItemId}")
    @Operation(
            summary = "Update shared item ciphertext",
            description = "Requires OWNER or PARTICIPANT with READ_WRITE permission and uses expectedVersion for OCC."
    )
    public ResponseEntity<UpdateSharedItemResponse> updateSharedItem(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long sharedItemId,
            @Valid @RequestBody UpdateSharedItemRequest request
    ) {
        return ResponseEntity.ok(sharedItemService.updateSharedItem(principal.getUserId(), sharedItemId, request));
    }

    @DeleteMapping("/{sharedItemId}")
    @Operation(summary = "Soft-delete a shared item. OWNER only.")
    public ResponseEntity<DeleteSharedItemResponse> deleteSharedItem(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long sharedItemId
    ) {
        return ResponseEntity.ok(sharedItemService.deleteSharedItem(principal.getUserId(), sharedItemId));
    }

    @PostMapping("/{sharedItemId}/invite-links")
    @Operation(
            summary = "Create a short-lived invite link. OWNER only.",
            description = "Returns the raw invite token once in inviteUrl. Only inviteTokenHash is stored."
    )
    public ResponseEntity<CreateInviteLinkResponse> createInviteLink(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long sharedItemId
    ) {
        return ResponseEntity.ok(sharedItemService.createInviteLink(principal.getUserId(), sharedItemId));
    }

    @PostMapping("/invite-links/{token}/join-requests")
    @Operation(
            summary = "Create a join request from an invite token",
            description = "Creates a pending request only. It does not grant access or receive sharedItemKey plaintext."
    )
    public ResponseEntity<CreateJoinRequestResponse> createJoinRequest(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable String token
    ) {
        return ResponseEntity.ok(sharedItemService.createJoinRequest(principal.getUserId(), token));
    }

    @GetMapping("/{sharedItemId}/join-requests")
    @Operation(summary = "List pending join requests for a shared item. OWNER only.")
    public ResponseEntity<List<PendingJoinRequestResponse>> getPendingJoinRequests(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long sharedItemId
    ) {
        return ResponseEntity.ok(sharedItemService.getPendingJoinRequests(principal.getUserId(), sharedItemId));
    }

    @PostMapping("/{sharedItemId}/join-requests/{joinRequestId}/approve")
    @Operation(
            summary = "Approve a pending join request. OWNER only.",
            description = "Stores encryptedItemKeyBase64 for the requester and conditionally marks the request APPROVED."
    )
    public ResponseEntity<JoinRequestDecisionResponse> approveJoinRequest(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long sharedItemId,
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
    @Operation(summary = "Reject a pending join request. OWNER only.")
    public ResponseEntity<JoinRequestDecisionResponse> rejectJoinRequest(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long sharedItemId,
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
            @PathVariable Long sharedItemId
    ) {
        return ResponseEntity.ok(sharedItemService.getMembers(principal.getUserId(), sharedItemId));
    }

    @PatchMapping("/{sharedItemId}/members/{memberId}/permission")
    @Operation(summary = "Update an active participant permission. OWNER only.")
    public ResponseEntity<UpdateSharedItemMemberPermissionResponse> updateMemberPermission(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long sharedItemId,
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
    @Operation(summary = "Revoke an active participant. OWNER only.")
    public ResponseEntity<RevokeSharedItemMemberResponse> revokeMember(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long sharedItemId,
            @PathVariable Long memberId
    ) {
        return ResponseEntity.ok(sharedItemService.revokeMember(principal.getUserId(), sharedItemId, memberId));
    }
}
