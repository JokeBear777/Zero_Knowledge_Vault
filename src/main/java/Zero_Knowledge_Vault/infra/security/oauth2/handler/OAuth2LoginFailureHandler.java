package Zero_Knowledge_Vault.infra.security.oauth2.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        log.error("🚨 OAuth2 login failed: {}", exception.getMessage(), exception);

        // OAuth2AuthenticationException이면 errorCode도 같이
        if (exception instanceof OAuth2AuthenticationException oae) {
            log.error("🚨 OAuth2 errorCode={}, description={}",
                    oae.getError().getErrorCode(),
                    oae.getError().getDescription());
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("text/plain; charset=UTF-8");
        response.getWriter().write("OAuth2 Login Failed: " + exception.getMessage());
    }

}
