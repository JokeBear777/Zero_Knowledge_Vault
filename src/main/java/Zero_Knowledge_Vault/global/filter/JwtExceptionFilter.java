package Zero_Knowledge_Vault.global.filter;

import Zero_Knowledge_Vault.global.util.dto.StatusResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtExceptionFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            filterChain.doFilter(request, response);
        } catch (JwtException e) {

            // 이미 응답이 커밋됐으면 아무것도 하지 않음
            if (response.isCommitted()) {
                return;
            }

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            objectMapper.writeValue(
                    response.getWriter(),
                    StatusResponseDto.addStatus(401)
            );

            response.flushBuffer();
            return;
        }
    }

}
