package Zero_Knowledge_Vault.infra.vault.policy;

import Zero_Knowledge_Vault.domain.vault.policy.VaultPolicy;
import Zero_Knowledge_Vault.global.crypto.kdf.KdfAlgorithm;
import Zero_Knowledge_Vault.global.crypto.kdf.KdfParams;
import org.springframework.stereotype.Component;

@Component
public class DefaultVaultPolicy implements VaultPolicy {

    @Override
    public KdfAlgorithm KdfAlgorithm() {
        return KdfAlgorithm.ARGON2ID;
    }

    @Override
    public KdfParams KdfParams() {
        return new KdfParams(
                65536,   // memory
                3,       // iterations
                1        // parallelism
        );
    }

    @Override
    public int version() {
        return 1;
    }
}
