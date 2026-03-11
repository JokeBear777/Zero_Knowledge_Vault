package Zero_Knowledge_Vault.domain.vault.policy;

import Zero_Knowledge_Vault.global.crypto.kdf.KdfAlgorithm;
import Zero_Knowledge_Vault.global.crypto.kdf.KdfParams;

public interface VaultPolicy {

    KdfAlgorithm KdfAlgorithm();

    KdfParams KdfParams();

    int version();
}
