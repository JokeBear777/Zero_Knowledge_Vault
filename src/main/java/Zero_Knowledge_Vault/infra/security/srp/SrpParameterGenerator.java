package Zero_Knowledge_Vault.infra.security.srp;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SrpParameterGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSalt(int byteLength) {
        byte[] salt = new byte[byteLength];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
}
