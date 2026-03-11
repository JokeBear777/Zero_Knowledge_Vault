package Zero_Knowledge_Vault.infra.QueryDsl.vault;

import Zero_Knowledge_Vault.domain.vault.repository.VaultIndexQueryRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class VaultIndexQueryRepositoryImpl implements VaultIndexQueryRepository {

    @Override
    public long updateIfVersionMatches(
            Long memberID,
            byte[] newIndexCipher,
            byte[] newCommitHash,
            long expectedIndexVersion,
            LocalDateTime now
    ) {




        return -1L;
    }
}
