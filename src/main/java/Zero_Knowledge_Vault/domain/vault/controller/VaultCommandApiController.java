package Zero_Knowledge_Vault.domain.vault.controller;

import Zero_Knowledge_Vault.domain.vault.dto.VaultItemDeleteRequest;
import Zero_Knowledge_Vault.domain.vault.dto.VaultItemUpsertRequest;
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
