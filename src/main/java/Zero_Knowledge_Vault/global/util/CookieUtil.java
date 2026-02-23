package Zero_Knowledge_Vault.global.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

import java.util.Arrays;

public class CookieUtil {
    private CookieUtil() {}

    public static void setAccessToken(HttpServletResponse response, String token, long maxAgeSeconds) {

        ResponseCookie cookie = ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(false)              // 로컬은 false, 운영 HTTPS면 true
                .path("/")
                .sameSite("Lax")            // 로컬은 Lax
                .maxAge(maxAgeSeconds)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    public static void clearAccessToken(HttpServletResponse response) {

        ResponseCookie cookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    public static String getAccessToken(HttpServletRequest request) {

        if (request.getCookies() == null) return null;

        return Arrays.stream(request.getCookies())
                .filter(cookie -> "access_token".equals(cookie.getName()))
                .map(cookie -> cookie.getValue())
                .findFirst()
                .orElse(null);
    }
}
