package Zero_Knowledge_Vault.infra.security.srp;

import Zero_Knowledge_Vault.domain.auth.policy.AuthPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SrpService {

    private final SrpParameterGenerator generator;
    private final AuthPolicy authPolicy;

    public SrpInitResponse initialize() {

        String salt = generator.generateSalt(16);

        return new SrpInitResponse(
                salt,
                SrpGroup.N.toString(16),
                SrpGroup.g.toString(),
                authPolicy.pakeAlgorithm().toString(),
                authPolicy.kdfAlgorithm().toString(),
                authPolicy.kdfParams()
        );

    }

}
