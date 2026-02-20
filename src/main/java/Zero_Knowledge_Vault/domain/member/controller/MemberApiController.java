package Zero_Knowledge_Vault.domain.member.controller;

import Zero_Knowledge_Vault.domain.member.dto.MeResponseDto;
import Zero_Knowledge_Vault.domain.member.service.MemberService;
import Zero_Knowledge_Vault.infra.security.jwt.CustomUserPrincipal;
import Zero_Knowledge_Vault.global.util.dto.RedirectResponseData;
import Zero_Knowledge_Vault.global.util.dto.StatusResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    @DeleteMapping("/me")
    public ResponseEntity<?> deActiveAccount(
            @AuthenticationPrincipal CustomUserPrincipal customUserPrincipal
    ) {
        memberService.deActiveAccount(customUserPrincipal);
        RedirectResponseData redirectResponseData = new RedirectResponseData(
                "Account deactivated. Redirect to login page.",
                "/"
        );
        return ResponseEntity.ok(StatusResponseDto.success(redirectResponseData));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(
            @AuthenticationPrincipal CustomUserPrincipal user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("인증되지 않은 사용자");
        }

        return ResponseEntity.ok(
                MeResponseDto.from(user)
        );
    }
}
