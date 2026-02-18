package Zero_Knowledge_Vault.global.security;

public enum AuthLevel {
    PRE_AUTH,
    VAULT_AUTH;

    @Override
    public String toString() {
        return name();
    }
}
