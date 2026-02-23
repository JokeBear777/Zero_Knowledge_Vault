package Zero_Knowledge_Vault.domain.auth.repository;

import Zero_Knowledge_Vault.domain.auth.entity.MemberAuthPake;

import java.util.Optional;

public interface MemberAuthPakeQueryRepository {
    Optional<MemberAuthPake> findActivePake(Long memberId);
}
