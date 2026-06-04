package Zero_Knowledge_Vault.domain.vault.share.controller;

import Zero_Knowledge_Vault.domain.vault.share.dto.request.CreateShareKeyRequest;
import Zero_Knowledge_Vault.domain.vault.share.dto.request.RegenerateShareKeyRequest;
import Zero_Knowledge_Vault.domain.vault.share.dto.response.DeleteShareKeyResponse;
import Zero_Knowledge_Vault.domain.vault.share.dto.response.MyShareKeyResponse;
import Zero_Knowledge_Vault.domain.vault.share.dto.response.PublicShareKeyResponse;
import Zero_Knowledge_Vault.domain.vault.share.service.MemberShareKeyService;
import Zero_Knowledge_Vault.infra.security.jwt.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/share/keys")
@RequiredArgsConstructor
@Tag(name = "Share Key", description = "Client-encrypted share key registration and lookup APIs")
public class MemberShareKeyApiController {

    private final MemberShareKeyService memberShareKeyService;

    /**
     * Returns the caller's active share key state.
     *
     * encryptedPrivateKeyBase64 is client-encrypted ciphertext. The server never receives
     * privateKey plaintext and treats the encrypted value as an opaque string.
     */
    @GetMapping("/me")
    @Operation(
            summary = "Get my share key state",
            description = "Returns the caller's ACTIVE share key if one exists. encryptedPrivateKeyBase64 is client-encrypted ciphertext, not privateKey plaintext."
    )
    public ResponseEntity<MyShareKeyResponse> getMyShareKey(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(memberShareKeyService.getMyShareKey(principal.getUserId()));
    }

    /**
     * Registers the caller's share key. publicKeyBase64 is stored for future itemKey sharing.
     * encryptedPrivateKeyBase64 must already be encrypted by the client.
     */
    @PostMapping
    @Operation(
            summary = "Register my share key",
            description = "Stores publicKeyBase64 and client-encrypted encryptedPrivateKeyBase64. The server never receives or decrypts privateKey plaintext."
    )
    public ResponseEntity<MyShareKeyResponse> createShareKey(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreateShareKeyRequest request
    ) {
        return ResponseEntity.ok(memberShareKeyService.createShareKey(principal.getUserId(), request));
    }

    /**
     * Returns another member's active public share key only.
     * encryptedPrivateKeyBase64 is intentionally not included in this response.
     */
    @GetMapping("/members/{memberId}")
    @Operation(
            summary = "Get another member's public share key",
            description = "Returns only the target member's ACTIVE publicKeyBase64. encryptedPrivateKeyBase64 is never included."
    )
    public ResponseEntity<PublicShareKeyResponse> getPublicShareKey(
            @PathVariable Long memberId
    ) {
        return ResponseEntity.ok(memberShareKeyService.getPublicShareKey(memberId));
    }

    /**
     * Soft-deletes the caller's active share key by marking it DELETED.
     */
    @DeleteMapping("/me")
    @Operation(
            summary = "Delete my share key",
            description = "Soft-deletes the caller's ACTIVE share key by changing its status to DELETED."
    )
    public ResponseEntity<DeleteShareKeyResponse> deleteMyShareKey(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(memberShareKeyService.deleteMyShareKey(principal.getUserId()));
    }

    /**
     * Rotates the existing active key to ROTATED and creates a new ACTIVE keyVersion.
     */
    @PostMapping("/regenerate")
    @Operation(
            summary = "Regenerate my share key",
            description = "Marks the existing ACTIVE key as ROTATED and creates a new ACTIVE keyVersion. Existing shared item re-encryption is out of scope."
    )
    public ResponseEntity<MyShareKeyResponse> regenerateShareKey(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody RegenerateShareKeyRequest request
    ) {
        return ResponseEntity.ok(memberShareKeyService.regenerateShareKey(principal.getUserId(), request));
    }
}
