package Zero_Knowledge_Vault.infra.security.jwt;

import Zero_Knowledge_Vault.domain.member.type.MemberRole;
import Zero_Knowledge_Vault.infra.security.AuthLevel;
import Zero_Knowledge_Vault.infra.security.config.JwtProperties;
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

    public GeneratedToken generateToken(Long memberId, String email, String memberRole, String authLevel) {
        String accessToken = generateAccessToken(memberId, email, memberRole, authLevel);

        return new GeneratedToken(accessToken);
    }

    public String generateAccessToken(Long memberId, String email ,String role, String auth) {
        long tokenPeriod = jwtProperties.getExpired();
        Claims claims = Jwts.claims().setSubject(String.valueOf(memberId));
        //claims.put("role", "ROLE_" + role);
        claims.put("email", email);
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

    public Long getUid(Claims claims) {
        return
                Long.parseLong(
                    claims.getSubject())
                ;
    }

    public MemberRole getRole(Claims claims) {
        return
                MemberRole.valueOf(
                        claims.get("role",String.class)
                );
    }

    public AuthLevel getAuthLevel(Claims claims) {
        return
                AuthLevel.valueOf(
                    claims.get("auth_level",String.class)
                );
    }

    public String getEmail(Claims claims) {
        return claims.get("email",String.class);
    }

    public Claims parse(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build().parseClaimsJws(token).getBody();
        } catch (JwtException e) {
            throw new JwtException("Invalid or expired token");
        }
    }




}
