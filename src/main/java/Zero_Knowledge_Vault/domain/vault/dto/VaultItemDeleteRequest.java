package Zero_Knowledge_Vault.domain.vault.dto;

public record VaultItemDeleteRequest(

        String itemId,
        long expectedItemVersion,
        long expectedIndexVersion,

        String newIndexCipherBase64,
        String newCommitHashBase64

) {
}
