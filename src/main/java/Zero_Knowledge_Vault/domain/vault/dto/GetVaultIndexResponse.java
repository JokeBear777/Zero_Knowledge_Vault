package Zero_Knowledge_Vault.domain.vault.dto;

import Zero_Knowledge_Vault.domain.vault.entity.VaultIndex;

import java.util.Base64;

public record GetVaultIndexResponse (
        String indexCipherBase64,
        String commitHashBase64,
        long version
){
    static public GetVaultIndexResponse from(VaultIndex vaultIndex) {
        String indexCipherBase64 = Base64.getEncoder().encodeToString(vaultIndex.getIndexCipher());
        String commitHashBase64 = Base64.getEncoder().encodeToString(vaultIndex.getCommitHash());

        return new GetVaultIndexResponse(indexCipherBase64, commitHashBase64, vaultIndex.getVersion());
    }

}
