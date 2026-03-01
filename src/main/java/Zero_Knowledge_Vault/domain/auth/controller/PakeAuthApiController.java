package Zero_Knowledge_Vault.domain.auth.controller;

import Zero_Knowledge_Vault.domain.auth.dto.PakeAuthInitRequest;
import Zero_Knowledge_Vault.domain.auth.dto.SrpAuthInitResponse;
import Zero_Knowledge_Vault.domain.auth.dto.SrpInitResponse;
import Zero_Knowledge_Vault.domain.auth.service.SrpAuthenticationService;
import Zero_Knowledge_Vault.infra.security.jwt.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pake/auth")
@RequiredArgsConstructor
public class PakeAuthApiController {

    private final SrpAuthenticationService srpAuthenticationService;

    @PostMapping("/init")
    public ResponseEntity<?> init(
            @AuthenticationPrincipal CustomUserPrincipal customUserPrincipal,
            @RequestBody PakeAuthInitRequest request
            ) {

        SrpAuthInitResponse res = srpAuthenticationService.initPake(request, customUserPrincipal);

        return ResponseEntity.ok(res);
    }

}
