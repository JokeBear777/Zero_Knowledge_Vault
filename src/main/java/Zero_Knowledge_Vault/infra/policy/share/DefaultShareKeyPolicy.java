package Zero_Knowledge_Vault.infra.policy.share;

import Zero_Knowledge_Vault.domain.share.policy.ShareKeyPolicy;
import org.springframework.stereotype.Component;

@Component
public class DefaultShareKeyPolicy implements ShareKeyPolicy {

    private static final String SUPPORTED_ALGORITHM = "RSA-OAEP-256";
    private static final int MAX_KEY_TEXT_LENGTH = 65_535;

    @Override
    public String supportedAlgorithm() {
        return SUPPORTED_ALGORITHM;
    }

    @Override
    public int maxKeyTextLength() {
        return MAX_KEY_TEXT_LENGTH;
    }
}