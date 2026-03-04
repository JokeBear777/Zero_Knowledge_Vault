package Zero_Knowledge_Vault.infra.auth.policy;

import Zero_Knowledge_Vault.domain.auth.policy.SrpPolicy;
import org.springframework.stereotype.Component;

@Component
public class DefaultSrpPolicy implements SrpPolicy {

    @Override
    public String hashAlgorithm() {
        return "SHA-256";
    }

}
