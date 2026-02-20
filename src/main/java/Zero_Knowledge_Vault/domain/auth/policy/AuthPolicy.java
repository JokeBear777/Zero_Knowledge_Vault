package Zero_Knowledge_Vault.domain.auth.policy;

import Zero_Knowledge_Vault.domain.auth.type.KdfAlgorithm;
import Zero_Knowledge_Vault.domain.auth.type.PakeAlgorithm;
import Zero_Knowledge_Vault.domain.auth.vo.KdfParams;

public interface AuthPolicy {

    PakeAlgorithm pakeAlgorithm();

    String groupId();

    KdfAlgorithm kdfAlgorithm();

    KdfParams kdfParams();

}
