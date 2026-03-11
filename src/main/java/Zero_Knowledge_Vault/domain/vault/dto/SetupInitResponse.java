package Zero_Knowledge_Vault.domain.vault.dto;

import Zero_Knowledge_Vault.global.crypto.kdf.KdfParams;

public record SetupInitResponse(
        String wrapKdfAlgorithm,
        KdfParams wrapKdfParams,
        String wrapSalt
) {
}
