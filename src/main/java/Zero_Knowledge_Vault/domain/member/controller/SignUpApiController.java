package Zero_Knowledge_Vault.domain.member.controller;

import Zero_Knowledge_Vault.domain.member.dto.OAuthSignupInfo;
import Zero_Knowledge_Vault.domain.member.dto.SignUpRequestDto;
import Zero_Knowledge_Vault.domain.member.service.MemberService;
import Zero_Knowledge_Vault.global.exception.custom.OAuthSignupSessionNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SignUpApiController {

    private final MemberService memberService;

    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp (
            @Valid @RequestBody SignUpRequestDto signUpRequestDto,
            HttpServletRequest request
    ) {
        HttpSession session = request.getSession(false);

        OAuthSignupInfo info = Optional.ofNullable(session)
                .map(s -> (OAuthSignupInfo) s.getAttribute("oauthSignupInfo"))
                .orElseThrow(OAuthSignupSessionNotFoundException::new);

        memberService.signUpOAuth(info, signUpRequestDto);

        session.removeAttribute("oauthSignupInfo");

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @GetMapping("/oauth-info")
    public ResponseEntity<?> getOAuthInfo(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            throw new RuntimeException("OAuth session not found");
        }

        OAuthSignupInfo info =
                (OAuthSignupInfo) session.getAttribute("oauthSignupInfo");

        if (info == null) {
            throw new RuntimeException("OAuth info not found");
        }

        return ResponseEntity.ok(info);
    }
}
