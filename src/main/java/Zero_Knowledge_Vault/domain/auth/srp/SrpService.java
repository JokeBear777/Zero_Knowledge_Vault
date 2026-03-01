package Zero_Knowledge_Vault.domain.auth.srp;

import Zero_Knowledge_Vault.domain.auth.dto.SrpChallenge;
import Zero_Knowledge_Vault.domain.auth.policy.AuthPolicy;
import Zero_Knowledge_Vault.domain.auth.policy.SrpPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class SrpService {

    private final SrpParameterGenerator generator;
    private final AuthPolicy authPolicy;
    private final SecureRandom secureRandom = new SecureRandom();
    private final SrpPolicy srpPolicy;

    public SrpRegisterResponse initialize() {

        String salt = generator.generateSalt(16);

        return new SrpRegisterResponse(
                salt,
                SrpGroup.N.toString(16),
                SrpGroup.g.toString(),
                authPolicy.pakeAlgorithm().toString(),
                authPolicy.kdfAlgorithm().toString(),
                authPolicy.kdfParams()
        );

    }

    public SrpChallenge generateChallenge(
            String Ahex,
            BigInteger verifier,
            BigInteger N,
            BigInteger g
    ) {
        BigInteger A = new BigInteger(Ahex, 16);
        byte[] bBytes = new byte[32];
        secureRandom.nextBytes(bBytes);
        BigInteger b = new BigInteger(1, bBytes);

        BigInteger k = computeK(N, g);

        BigInteger gb = g.modPow(b, N);

        BigInteger B = k.multiply(verifier).add(gb).mod(N);

        return new SrpChallenge(
                B.toString(16),
                b.toString(16)
        );
    }

    private BigInteger computeK(BigInteger N, BigInteger g) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance(srpPolicy.hashAlgorithm());
            sha256.update(N.toByteArray());
            sha256.update(g.toByteArray());
            return new BigInteger(1, sha256.digest());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



}
