package Zero_Knowledge_Vault.infra.security;

public enum AuthLevel {
    PRE_AUTH,
    VAULT_AUTH;

    @Override
    public String toString() {
        return name();
    }
}
