package Zero_Knowledge_Vault.domain.auth.controller;

import Zero_Knowledge_Vault.domain.auth.dto.*;
import Zero_Knowledge_Vault.domain.auth.service.SrpAuthenticationService;
import Zero_Knowledge_Vault.global.util.CookieUtil;
import Zero_Knowledge_Vault.infra.security.jwt.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pake/auth")
@RequiredArgsConstructor
@Slf4j
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

    @PostMapping("/prove")
    public ResponseEntity<?> prove(
            @AuthenticationPrincipal CustomUserPrincipal customUserPrincipal,
            SrpAuthProveRequest request,
            HttpServletResponse response
    ) {
        ProveResult result =
            srpAuthenticationService.prove(customUserPrincipal.getUserId(), request.authSessionId(), request.M1());

        CookieUtil.setAccessToken(
                response,
                result.elevationToken(),
                (int) result.expiresInSeconds()
        );

        return ResponseEntity.ok().build();
    }

}
