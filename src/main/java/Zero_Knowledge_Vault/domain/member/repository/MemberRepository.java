package Zero_Knowledge_Vault.domain.member.repository;

import Zero_Knowledge_Vault.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
