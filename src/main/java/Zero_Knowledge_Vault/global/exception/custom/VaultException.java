package Zero_Knowledge_Vault.global.exception.custom;

import Zero_Knowledge_Vault.global.exception.type.VaultErrorCode;
import lombok.Getter;

public class VaultException extends RuntimeException {
    private final VaultErrorCode errorCode;

    public VaultException(VaultErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public VaultErrorCode getErrorCode() {
        return errorCode;
    }
}
