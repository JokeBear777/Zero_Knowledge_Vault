package Zero_Knowledge_Vault.domain.vault.repository;

import java.time.LocalDateTime;

public interface VaultItemQueryRepository {
     boolean updateIfVersionMatches(
            Long memberId,
            String itemId,
            byte[] itemKeyCipher,
            byte[] itemCipher,
            long version,
            LocalDateTime now
    );

    boolean tombstoneIfVersionMatches(
            Long memberId,
            String itemId,
            long version,
            LocalDateTime now
    );
}
