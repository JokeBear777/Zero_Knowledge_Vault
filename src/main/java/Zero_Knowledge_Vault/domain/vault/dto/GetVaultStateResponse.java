package Zero_Knowledge_Vault.domain.vault.dto;

public record GetVaultStateResponse(
        boolean vaultKeyMaterialExists,
        boolean vaultIndexExists
) {
}
