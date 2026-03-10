package Zero_Knowledge_Vault.infra.QueryDsl.vault;

import Zero_Knowledge_Vault.domain.vault.repository.VaultItemQueryRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

import static Zero_Knowledge_Vault.domain.vault.entity.QVaultItem.vaultItem;

@Repository
@RequiredArgsConstructor
public class VaultItemQueryRepositoryImpl implements VaultItemQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public boolean updateIfVersionMatches(
            Long memberId,
            String itemId,
            byte[] itemKeyCipher,
            byte[] itemCipher,
            long version,
            LocalDateTime now
    ) {
        long updatedCount = queryFactory
                .update(vaultItem)
                .set(vaultItem.itemKeyCipher, itemKeyCipher)
                .set(vaultItem.itemCipher, itemCipher)
                .set(vaultItem.version, vaultItem.version.add(1))
                .set(vaultItem.deletedAt, (LocalDateTime) null)
                .set(vaultItem.updatedAt, now)
                .where(
                        vaultItem.memberId.eq(memberId),
                        vaultItem.itemId.eq(itemId),
                        vaultItem.version.eq(version)
                )
                .execute();

        return updatedCount == 1;
    }

    @Override
    public boolean tombstoneIfVersionMatches(
            Long memberId,
            String itemId,
            long version,
            LocalDateTime now
    ) {
        long updatedCount = queryFactory
                .update(vaultItem)
                .set(vaultItem.deletedAt, now)
                .set(vaultItem.version, vaultItem.version.add(1))
                .set(vaultItem.updatedAt, now)
                .where(
                        vaultItem.memberId.eq(memberId),
                        vaultItem.itemId.eq(itemId),
                        vaultItem.version.eq(version),
                        vaultItem.deletedAt.isNull()
                )
                .execute();

        return updatedCount == 1;
    }
}
