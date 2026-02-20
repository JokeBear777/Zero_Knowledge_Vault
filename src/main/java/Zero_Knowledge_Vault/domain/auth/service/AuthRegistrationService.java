package Zero_Knowledge_Vault.domain.auth.service;

import Zero_Knowledge_Vault.domain.auth.dto.RegisterAuthRequest;
import Zero_Knowledge_Vault.domain.auth.policy.AuthPolicy;
import Zero_Knowledge_Vault.domain.auth.repository.MemberAuthPakeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthRegistrationService {

    private final AuthPolicy authPolicy;
    private final MemberAuthPakeRepository repository;


    public void register(RegisterAuthRequest registerAuthRequest) {


    }

}
