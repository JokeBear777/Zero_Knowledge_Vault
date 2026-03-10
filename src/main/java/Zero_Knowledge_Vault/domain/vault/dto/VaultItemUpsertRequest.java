package Zero_Knowledge_Vault.domain.vault.dto;

public record VaultItemUpsertRequest(

        String itemId,
        Long expectedItemVersion,
        Long expectedIndexVersion,

        String itemKeyCipherBase64,
        String itemCipherBase64,

        String newIndexCipherBase64,
        String newCommitHashBase64

) {
}
