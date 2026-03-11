package Zero_Knowledge_Vault.domain.vault.controller;

import Zero_Knowledge_Vault.domain.vault.dto.*;
import Zero_Knowledge_Vault.domain.vault.service.VaultCommandService;
import Zero_Knowledge_Vault.infra.security.jwt.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vault")
@RequiredArgsConstructor
public class VaultCommandApiController {

    private final VaultCommandService vaultCommandService;

    @GetMapping("/setup/init")
    public ResponseEntity<SetupInitResponse> getSetupInit(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        SetupInitResponse res = vaultCommandService.getSetupInitResponse(principal.getUserId());

        return ResponseEntity.ok(res);
    }

    @PostMapping("/setup")
    public ResponseEntity<Void> setUpVault(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestBody SetupVaultKeyRequest request
            ) {

        vaultCommandService.setupVault(principal.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/setup/index")
    public ResponseEntity<?> setUpIndex(
            @AuthenticationPrincipal CustomUserPrincipal customUserPrincipal,
            @RequestBody SetupVaultIndexRequest request
            ) {
        vaultCommandService.setupVaultIndex(customUserPrincipal.getUserId(),request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/item")
    public ResponseEntity<Void> upsertItem(
            @AuthenticationPrincipal CustomUserPrincipal customUserPrincipal,
            @RequestBody VaultItemUpsertRequest request
            ) {

        vaultCommandService.upsertItem(customUserPrincipal.getUserId(), request);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/item")
    public ResponseEntity<Void> deleteItem(
            @AuthenticationPrincipal CustomUserPrincipal customUserPrincipal,
            @RequestBody VaultItemDeleteRequest request
    ) {

        vaultCommandService.deleteItem(customUserPrincipal.getUserId(), request);

        return ResponseEntity.ok().build();
    }

}
