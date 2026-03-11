package Zero_Knowledge_Vault.domain.vault.repository;

import Zero_Knowledge_Vault.domain.vault.entity.VaultIndex;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VaultIndexRepository extends JpaRepository<VaultIndex, Long> {
    Optional<VaultIndex> findByMemberId(Long memberId);
}
