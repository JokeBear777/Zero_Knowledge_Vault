package Zero_Knowledge_Vault.domain.auth.controller;

import Zero_Knowledge_Vault.domain.auth.dto.RegisterAuthRequest;
import Zero_Knowledge_Vault.domain.auth.service.AuthRegistrationService;
import Zero_Knowledge_Vault.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class MemberAuthPakeApiController {

    AuthRegistrationService authRegistrationService;

    @PostMapping("/register")
    public void register(
            @RequestBody RegisterAuthRequest registerAuthRequest,
            @AuthenticationPrincipal Member member
            ) {

        authRegistrationService.register(registerAuthRequest);
    }

}
