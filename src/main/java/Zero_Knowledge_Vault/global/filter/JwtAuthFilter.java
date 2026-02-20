package Zero_Knowledge_Vault.global.filter;
import Zero_Knowledge_Vault.domain.member.entity.Member;
import Zero_Knowledge_Vault.domain.member.service.MemberService;
import Zero_Knowledge_Vault.domain.member.type.MemberRole;
import Zero_Knowledge_Vault.infra.security.AuthLevel;
import Zero_Knowledge_Vault.infra.security.jwt.JwtUtil;
import Zero_Knowledge_Vault.infra.security.jwt.CustomUserPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final MemberService memberService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        log.info("doFilterInternal start");

        String path = request.getServletPath();
        log.info("Request path: {}", path);
        // 모두 허용 URL 처리
        if (path.startsWith("/login")
                || path.startsWith("/error")
                || path.startsWith("/oauth2-login")
                || path.startsWith("/sign-up")
                || path.startsWith("/js")
                || path.startsWith("/css")
                || path.startsWith("/favicon")) {

            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = request.getHeader("Authorization");

        if (!StringUtils.hasText(accessToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7).trim();
        }

        if (!jwtUtil.verifyToken(accessToken)) {
            throw new JwtException("Access token is invalid or expired");
        }

        if (jwtUtil.verifyToken(accessToken)) {

            Claims claims = jwtUtil.parse(accessToken);

            Long memberId = jwtUtil.getUid(claims);
            String email = jwtUtil.getEmail(claims);
            MemberRole memberRole = jwtUtil.getRole(claims);
            AuthLevel authLevel = jwtUtil.getAuthLevel(claims);

            /*
            Member findMember =  memberService.findById(memberId)
                    .orElseThrow(IllegalStateException::new);
             */
            CustomUserPrincipal userDto = CustomUserPrincipal.builder()
                    .userId(memberId)
                    .email(email)
                    .role(memberRole)
                    .authLevel(authLevel)
                    .build();

            Authentication auth = getAuthentication(userDto);
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.info("verification is success");
        }

        filterChain.doFilter(request, response);
    }


    public Authentication getAuthentication(CustomUserPrincipal member) {


        return new UsernamePasswordAuthenticationToken(member, "",
                List.of(
                        new SimpleGrantedAuthority(member.getRole().toString()),
                        new SimpleGrantedAuthority(member.getAuthLevel().toString())
                )
        );
    }
}
