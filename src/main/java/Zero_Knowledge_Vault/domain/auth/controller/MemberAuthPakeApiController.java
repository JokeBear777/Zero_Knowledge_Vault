package Zero_Knowledge_Vault.domain.auth.controller;


import Zero_Knowledge_Vault.domain.auth.dto.RegisterAuthRequest;
import Zero_Knowledge_Vault.domain.auth.service.AuthRegistrationService;
import Zero_Knowledge_Vault.domain.member.entity.Member;
import Zero_Knowledge_Vault.infra.security.jwt.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MemberAuthPakeApiController {

    AuthRegistrationService authRegistrationService;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterAuthRequest registerAuthRequest,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
            ) {

        authRegistrationService.register(registerAuthRequest);

        return null;
    }


}
