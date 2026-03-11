package Zero_Knowledge_Vault.domain.auth.policy;

import Zero_Knowledge_Vault.global.crypto.kdf.KdfAlgorithm;
import Zero_Knowledge_Vault.domain.auth.type.PakeAlgorithm;
import Zero_Knowledge_Vault.global.crypto.kdf.KdfParams;

public interface AuthPolicy {

    PakeAlgorithm pakeAlgorithm();

    String groupId();

    KdfAlgorithm kdfAlgorithm();

    KdfParams kdfParams();

    int version();

}
