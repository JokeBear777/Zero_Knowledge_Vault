package Zero_Knowledge_Vault.global.security.jwt;

import Zero_Knowledge_Vault.global.security.config.JwtProperties;
import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtUtil {
    private final JwtProperties jwtProperties;
    private Key secretKey;

    @PostConstruct
    protected void init() {
        secretKey = jwtProperties.getSecretKey();
    }

    public GeneratedToken generateToken(String email ,String role, String auth) {
        String accessToken = generateAccessToken(email, role, auth);

        return new GeneratedToken(accessToken);
    }

    public String generateAccessToken(String email,String role, String auth) {
        long tokenPeriod = jwtProperties.getExpired();
        Claims claims = Jwts.claims().setSubject(email);
        //claims.put("role", "ROLE_" + role);
        claims.put("role", role);
        claims.put("auth_level", auth);

        Date now = new Date();
        return
                Jwts.builder()
                        .setClaims(claims)
                        .setIssuedAt(now)
                        .setExpiration(new Date(now.getTime() + tokenPeriod))
                        .signWith(secretKey,SignatureAlgorithm.HS256)
                        .compact();
    }

    public boolean verifyToken(String token) {
        log.info("String token = {}", token);
        try{
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build().parseClaimsJws(token);
            log.info(claims.getBody().toString());
            return claims.getBody()
                    .getExpiration()
                    .after(new Date());
        } catch (ExpiredJwtException e) {
            log.error("JWT expired: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.security.SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.error("Malformed JWT: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            return false;
        }
    }

    public String getUid(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build().parseClaimsJws(token).getBody().getSubject();
    }

    public String getRole(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build().parseClaimsJws(token).getBody().get("role",String.class);
    }

    public String getAuthLevel(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build().parseClaimsJws(token).getBody().get("auth_level",String.class);
    }



}
