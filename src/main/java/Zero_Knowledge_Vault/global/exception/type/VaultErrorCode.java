package Zero_Knowledge_Vault.global.exception.type;

public enum VaultErrorCode {
    ITEM_NOT_FOUND("Vault item not found"),
    ITEM_ALREADY_DELETED("Vault item already deleted"),
    ITEM_VERSION_CONFLICT("Vault item version conflict"),
    INDEX_VERSION_CONFLICT("Vault index version conflict");

    private final String message;

    VaultErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
