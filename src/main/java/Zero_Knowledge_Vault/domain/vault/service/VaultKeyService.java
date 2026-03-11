package Zero_Knowledge_Vault.domain.vault.service;

import Zero_Knowledge_Vault.domain.vault.dto.SetupInitResponse;
import Zero_Knowledge_Vault.domain.vault.policy.VaultPolicy;
import Zero_Knowledge_Vault.domain.vault.util.VaultParameterGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VaultKeyService {

    private final VaultParameterGenerator generator;
    private final VaultPolicy policy;

    public SetupInitResponse initialize() {

        String salt = generator.generateSalt(16);

        return new SetupInitResponse(
                policy.KdfAlgorithm().toString(),
                policy.KdfParams(),
                salt
        );

    }

}
