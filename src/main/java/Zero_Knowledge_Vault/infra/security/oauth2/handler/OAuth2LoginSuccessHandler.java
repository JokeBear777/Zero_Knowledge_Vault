package Zero_Knowledge_Vault.infra.security.oauth2.handler;

import Zero_Knowledge_Vault.domain.member.dto.OAuthSignupInfo;
import Zero_Knowledge_Vault.domain.member.entity.Member;
import Zero_Knowledge_Vault.domain.member.service.MemberService;
import Zero_Knowledge_Vault.global.util.CookieUtil;
import Zero_Knowledge_Vault.infra.security.AuthLevel;
import Zero_Knowledge_Vault.infra.security.jwt.GeneratedToken;
import Zero_Knowledge_Vault.infra.security.jwt.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final MemberService memberService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String provider = oAuth2User.getAttribute("provider");
        String name = oAuth2User.getAttribute("name");
        String mobile = oAuth2User.getAttribute("mobile");

        boolean isExist = Boolean.TRUE.equals(oAuth2User.getAttribute("isExist"));

        if(isExist) {
            Member member = memberService.findByEmail(email)
                    .orElseThrow(IllegalAccessError::new);

            GeneratedToken accessToken = jwtUtil.generateToken(
                    member.getMemberId(),
                    email,
                    member.getMemberRole().toString(),
                    AuthLevel.PRE_AUTH.toString()
            );

            /*
            String redirectUrl = UriComponentsBuilder
                    .fromUriString("/?token=" + token.getAccessToken())
                    .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            */
            CookieUtil.setAccessToken(response, accessToken.getAccessToken(), 60 * 10);

            response.sendRedirect("/dashboard.html");
        }

        if(!isExist) {

            OAuthSignupInfo oAuthSignupInfo = new OAuthSignupInfo(
                    email,
                    provider,
                    name,
                    mobile
            );

            request.getSession().setAttribute("oauthSignupInfo", oAuthSignupInfo);

            response.sendRedirect("/sign-up.html");
        }

    }
}
