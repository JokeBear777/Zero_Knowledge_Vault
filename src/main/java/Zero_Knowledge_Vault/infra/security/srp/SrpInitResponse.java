package Zero_Knowledge_Vault.infra.security.srp;

import Zero_Knowledge_Vault.domain.auth.vo.KdfParams;

public record SrpInitResponse(
        String saltAuth,
        String N,
        String g,
        String hashAlgorithm,
        String kdfAlgorithm,
        KdfParams kdfParams
) {
}
