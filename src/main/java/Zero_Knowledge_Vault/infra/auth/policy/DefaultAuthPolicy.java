package Zero_Knowledge_Vault.infra.auth.policy;

import Zero_Knowledge_Vault.domain.auth.policy.AuthPolicy;
import Zero_Knowledge_Vault.domain.auth.type.KdfAlgorithm;
import Zero_Knowledge_Vault.domain.auth.type.PakeAlgorithm;
import Zero_Knowledge_Vault.domain.auth.vo.KdfParams;
import org.springframework.stereotype.Component;

@Component
public class DefaultAuthPolicy implements AuthPolicy {

    @Override
    public PakeAlgorithm pakeAlgorithm() {
        return PakeAlgorithm.SRP_6A;
    }

    @Override
    public String groupId() {
        return "RFC5054_2048";
    }

    @Override
    public KdfAlgorithm kdfAlgorithm() {
        return KdfAlgorithm.ARGON2ID;
    }

    @Override
    public KdfParams kdfParams() {
        return new KdfParams(
                65536,   // memory
                3,       // iterations
                1        // parallelism
        );
    }
}
