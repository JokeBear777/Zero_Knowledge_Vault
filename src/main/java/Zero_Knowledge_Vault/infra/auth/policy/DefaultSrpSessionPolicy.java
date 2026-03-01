package Zero_Knowledge_Vault.infra.auth.policy;

import Zero_Knowledge_Vault.domain.auth.policy.SrpSessionPolicy;
import org.springframework.stereotype.Component;

@Component
public class DefaultSrpSessionPolicy implements SrpSessionPolicy {

    @Override
    public Long ttlSeconds() {
        return 180L;
    }

}
