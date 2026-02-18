package Zero_Knowledge_Vault.global.security.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {

    @PostConstruct
    public void check() {
        System.out.println("secretKey = " + secretKey);
    }

    private String issuer;
    private String secretKey;
    private Long expired;

    public Key getSecretKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("Secret key length must be at least 256 bits (32 bytes)");
        }

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }
}
