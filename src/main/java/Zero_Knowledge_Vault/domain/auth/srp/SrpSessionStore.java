package Zero_Knowledge_Vault.domain.auth.srp;

import java.util.Optional;

// TODO: consider refactoring to BigInteger at interface level
public interface SrpSessionStore {

    SrpSession create(
            Long userId,
            String AHex,
            String BHex,
            String bHex,
            Long ttlSeconds
    );

    Optional<SrpSession> findValid(String sessionId);

    void markUsed(String sessionId);

    void delete(String sessionId);
}
