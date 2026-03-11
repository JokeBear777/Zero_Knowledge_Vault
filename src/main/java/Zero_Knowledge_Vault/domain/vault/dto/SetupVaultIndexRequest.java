package Zero_Knowledge_Vault.domain.vault.dto;

public record SetupVaultIndexRequest(
    String indexCipherBase64,
    String commitHashBase64
) {
}
