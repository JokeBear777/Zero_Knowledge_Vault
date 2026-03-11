package Zero_Knowledge_Vault.domain.vault.repository;

import Zero_Knowledge_Vault.domain.vault.entity.VaultItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VaultItemRepository extends JpaRepository<VaultItem, Long> {

    Optional<VaultItem> findByMemberIdAndItemId(Long memberId, String itemId);
}
