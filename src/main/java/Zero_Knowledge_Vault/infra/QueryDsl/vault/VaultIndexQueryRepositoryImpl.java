package Zero_Knowledge_Vault.infra.QueryDsl.vault;

import Zero_Knowledge_Vault.domain.vault.repository.VaultIndexQueryRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

import static Zero_Knowledge_Vault.domain.vault.entity.QVaultIndex.vaultIndex;

@Repository
@RequiredArgsConstructor
public class VaultIndexQueryRepositoryImpl implements VaultIndexQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public long updateIfVersionMatches(
            Long memberID,
            byte[] newIndexCipher,
            byte[] newCommitHash,
            long expectedIndexVersion,
            LocalDateTime now
    ) {
        return queryFactory
                .update(vaultIndex)
                .set(vaultIndex.indexCipher, newIndexCipher)
                .set(vaultIndex.commitHash, newCommitHash)
                .set(vaultIndex.version, vaultIndex.version.add(1))
                .set(vaultIndex.updatedAt, now)
                .where(
                        vaultIndex.memberId.eq(memberID),
                        vaultIndex.version.eq(expectedIndexVersion)
                )
                .execute();
    }
}
