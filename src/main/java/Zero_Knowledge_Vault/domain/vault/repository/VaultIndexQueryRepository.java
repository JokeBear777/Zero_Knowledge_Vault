package Zero_Knowledge_Vault.domain.vault.repository;

import java.time.LocalDateTime;

public interface VaultIndexQueryRepository {
    long updateIfVersionMatches(
            Long memberID,
            byte[] newIndexCipher,
            byte[] newCommitHash,
            long expectedIndexVersion,
            LocalDateTime now
    );
}
