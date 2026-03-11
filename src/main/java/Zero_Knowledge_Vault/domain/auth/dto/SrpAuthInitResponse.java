package Zero_Knowledge_Vault.domain.auth.dto;

import Zero_Knowledge_Vault.global.crypto.kdf.KdfParams;

public record SrpAuthInitResponse(
        String authSessionId,   // UUID (String)
        String saltAuth,        // base64
        String B,               // hex
        String group,           // "4096"
        String hash,            // "SHA-256"
        String kdfAlgorithm,    // "ARGON2ID"
        KdfParams kdfParams     // JSON 객체
) {
}
