package Zero_Knowledge_Vault.domain.vault.dto;

import Zero_Knowledge_Vault.global.crypto.kdf.KdfAlgorithm;
import Zero_Knowledge_Vault.global.crypto.kdf.KdfParams;

public record SetupVaultKeyRequest(
        String saltWrapBase64,
        String wrappedVaultKeyBase64,
        KdfAlgorithm wrapKdfAlgorithm,
        KdfParams wrapKdfParams,
        String indexCipherBase64,
        String commitHashBase64
) {
}
