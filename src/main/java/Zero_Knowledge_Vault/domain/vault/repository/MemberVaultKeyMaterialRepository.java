package Zero_Knowledge_Vault.domain.vault.repository;

import Zero_Knowledge_Vault.domain.vault.entity.MemberVaultKeyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberVaultKeyMaterialRepository extends JpaRepository<MemberVaultKeyMaterial, Long> {
}
