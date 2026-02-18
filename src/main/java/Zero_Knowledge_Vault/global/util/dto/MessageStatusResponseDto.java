package Zero_Knowledge_Vault.global.util.dto;

import lombok.Getter;

@Getter
public class MessageStatusResponseDto extends BaseResponseDto {
    private String message;

    public MessageStatusResponseDto(Integer status, String message) {
        super(status);
        this.message = message;
    }

    public MessageStatusResponseDto(Integer status, String message, Object data) {
        super(status, data);
        this.message = message;
    }

    public static MessageStatusResponseDto success(String message) {
        return new MessageStatusResponseDto(200, message);
    }

    public static MessageStatusResponseDto success(String message, Object data) {
        return new MessageStatusResponseDto(200, message, data);
    }
}
