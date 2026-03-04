package Zero_Knowledge_Vault.domain.auth.srp;

import Zero_Knowledge_Vault.domain.auth.dto.SrpChallenge;
import Zero_Knowledge_Vault.domain.auth.policy.AuthPolicy;
import Zero_Knowledge_Vault.domain.auth.policy.SrpPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class SrpService {

    private final SrpParameterGenerator generator;
    private final AuthPolicy authPolicy;
    private final SecureRandom secureRandom = new SecureRandom();
    private final SrpPolicy srpPolicy;
    private final SrpSessionStore srpSessionStore;

    public SrpRegisterResponse initialize() {

        String salt = generator.generateSalt(16);

        return new SrpRegisterResponse(
                salt,
                SrpGroup.N.toString(16),
                SrpGroup.g.toString(),
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

    public String verify(BigInteger v, SrpSession session, String clientM1Hex) {
        BigInteger N = SrpGroup.N;

        BigInteger A = parseHexToBigInt(session.AHex());
        BigInteger B = parseHexToBigInt(session.BHex());
        BigInteger b = parseHexToBigInt(session.bHex());

        if (A.mod(N).equals(BigInteger.ZERO)) {
            srpSessionStore.delete(session.id());
            throw new SecurityException("Invalid A");
        }

        BigInteger u = computeU(A, B);

        BigInteger S = A.multiply(v.modPow(u, N))
                .mod(N)
                .modPow(b, N);

        byte[] K = hashSessionKey(S);

        String expectedM1Hex = computeM1Hex(A, B, K);

        log.info("Server S: {}", S.toString(16));
        log.info("Server K: {}", bytesToHex(K));
        log.info("Server M1: {}", expectedM1Hex);
        log.info("Client M1: {}", clientM1Hex);

        if (!constantTimeEqualsHex(expectedM1Hex, clientM1Hex)) {
            srpSessionStore.delete(session.id());
            throw new SecurityException("Invalid SRP proof (M1)");
        }

        String m2Hex = computeM2Hex(A, clientM1Hex, K);

        srpSessionStore.delete(session.id());

        return m2Hex;
    }

    private BigInteger parseHexToBigInt(String hex) {
        if (hex == null || hex.isBlank()) {
            throw new IllegalArgumentException("Hex string is null or empty");
        }

        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }

        return new BigInteger(hex, 16);
    }

    private boolean constantTimeEqualsHex(String aHex, String bHex) {
        byte[] a = hexToBytes(aHex);
        byte[] b = hexToBytes(bHex);

        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }

        return result == 0;
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) (
                    (Character.digit(hex.charAt(i), 16) << 4)
                            + Character.digit(hex.charAt(i + 1), 16)
            );
        }

        return data;
    }

    private BigInteger computeU(BigInteger A, BigInteger B) {
        byte[] aBytes = toPaddedBytes(A);
        byte[] bBytes = toPaddedBytes(B);

        byte[] combined = concat(aBytes, bBytes);

        byte[] hash = sha256(combined);

        return new BigInteger(1, hash);
    }

    private byte[] hashSessionKey(BigInteger S) {
        byte[] sBytes = toPaddedBytes(S);
        return sha256(sBytes);
    }

    private String computeM1Hex(BigInteger A, BigInteger B, byte[] K) {

        byte[] aBytes = toPaddedBytes(A);
        byte[] bBytes = toPaddedBytes(B);

        byte[] combined = concat(aBytes, bBytes, K);

        byte[] hash = sha256(combined);

        return bytesToHex(hash);
    }

    private String computeM2Hex(BigInteger A, String m1Hex, byte[] K) {

        byte[] aBytes = toPaddedBytes(A);
        byte[] m1Bytes = hexToBytes(m1Hex);

        byte[] combined = concat(aBytes, m1Bytes, K);

        byte[] hash = sha256(combined);

        return bytesToHex(hash);
    }

    private byte[] sha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] toPaddedBytes(BigInteger value) {
        byte[] bytes = value.toByteArray();

        if (bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }

        int length = (SrpGroup.N.bitLength() + 7) / 8;

        if (bytes.length < length) {
            byte[] padded = new byte[length];
            System.arraycopy(bytes, 0, padded, length - bytes.length, bytes.length);
            return padded;
        }

        return bytes;
    }

    private byte[] concat(byte[]... arrays) {
        int totalLength = 0;
        for (byte[] arr : arrays) {
            totalLength += arr.length;
        }

        byte[] result = new byte[totalLength];
        int pos = 0;

        for (byte[] arr : arrays) {
            System.arraycopy(arr, 0, result, pos, arr.length);
            pos += arr.length;
        }

        return result;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

}
