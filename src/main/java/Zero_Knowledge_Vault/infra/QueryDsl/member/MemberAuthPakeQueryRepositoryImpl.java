package Zero_Knowledge_Vault.infra.QueryDsl.member;

import Zero_Knowledge_Vault.domain.auth.entity.MemberAuthPake;
import Zero_Knowledge_Vault.domain.auth.entity.QMemberAuthPake;
import Zero_Knowledge_Vault.domain.auth.repository.MemberAuthPakeQueryRepository;
import Zero_Knowledge_Vault.domain.auth.type.PakeAuthStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberAuthPakeQueryRepositoryImpl implements MemberAuthPakeQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<MemberAuthPake> findActivePake(Long memberId) {

        QMemberAuthPake pake = QMemberAuthPake.memberAuthPake;

        return Optional.ofNullable(
                queryFactory
                        .selectFrom(pake)
                        .where(pake.memberId.eq(memberId))
                        .fetchOne()
        );
    }
}
