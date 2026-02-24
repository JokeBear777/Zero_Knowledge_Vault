package Zero_Knowledge_Vault.domain.auth.controller;

import Zero_Knowledge_Vault.domain.auth.dto.PakeRegisterRequest;
import Zero_Knowledge_Vault.domain.auth.dto.PakeRegisterResponse;
import Zero_Knowledge_Vault.domain.auth.service.AuthRegistrationService;
import Zero_Knowledge_Vault.infra.security.jwt.CustomUserPrincipal;
import Zero_Knowledge_Vault.infra.security.srp.SrpInitResponse;
import Zero_Knowledge_Vault.infra.security.srp.SrpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pake")
@RequiredArgsConstructor
public class MemberAuthPakeApiController {

    private final AuthRegistrationService authRegistrationService;
    private final SrpService srpService;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody PakeRegisterRequest pakeRegisterRequest,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
            ) {

        PakeRegisterResponse res =
                authRegistrationService.register(pakeRegisterRequest, userPrincipal.getUserId());

        return ResponseEntity.ok(res);
    }

    @GetMapping("/register")
    public ResponseEntity<?> getPakeInit(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        SrpInitResponse res = srpService.initialize();
        return ResponseEntity.ok(res);
    }

}
