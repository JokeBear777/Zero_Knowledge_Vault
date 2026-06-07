package Zero_Knowledge_Vault.global.exception.custom;

import org.springframework.http.HttpStatus;

public class ShareKeyException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ShareKeyException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
