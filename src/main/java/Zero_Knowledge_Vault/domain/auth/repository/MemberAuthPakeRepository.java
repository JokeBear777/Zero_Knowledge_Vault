package Zero_Knowledge_Vault.domain.auth.repository;

import Zero_Knowledge_Vault.domain.auth.entity.MemberAuthPake;
import jakarta.security.auth.message.AuthStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberAuthPakeRepository extends JpaRepository<MemberAuthPake, Long> {
    Optional<MemberAuthPake> findByMemberId(Long memberId);
}
