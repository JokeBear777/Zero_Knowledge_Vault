package Zero_Knowledge_Vault.domain.auth.srp;

import Zero_Knowledge_Vault.domain.auth.vo.KdfParams;

public record SrpRegisterResponse(
        String saltAuth,
        String N,
        String g,
        String hashAlgorithm,
        String kdfAlgorithm,
        KdfParams kdfParams
) {
}
