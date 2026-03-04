package Zero_Knowledge_Vault.domain.auth.srp;

import java.time.Instant;

public record SrpSession(
        String id,
        Long userId,
        String AHex,
        String BHex,
        String bHex,         // 서버 비밀값(세션키 계산에 필요) - 구현에 맞게 보관
        Instant expiresAt,
        boolean used
) {
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

}
