package Zero_Knowledge_Vault.domain.auth.repository;

import Zero_Knowledge_Vault.domain.auth.entity.MemberAuthPake;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberAuthPakeRepository extends JpaRepository<MemberAuthPake, Long> {
}
