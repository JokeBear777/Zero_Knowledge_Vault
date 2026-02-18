package Zero_Knowledge_Vault.global.exception.custom;

public class OAuthSignupSessionNotFoundException extends RuntimeException {
    public OAuthSignupSessionNotFoundException() {
        super("OAuth signup session not found");
    }
}
