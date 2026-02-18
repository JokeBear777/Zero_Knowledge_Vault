package Zero_Knowledge_Vault.global.util.dto;

import lombok.Getter;

@Getter
public class StatusResponseDto extends BaseResponseDto{

    public StatusResponseDto(Integer status) {
        super(status);
    }

    public StatusResponseDto(Integer status, Object data) {
        super(status, data);
    }

    public static StatusResponseDto success() {
        return new StatusResponseDto(200);
    }

    public static StatusResponseDto success(Object data) {
        return new StatusResponseDto(200, data);
    }

    public static StatusResponseDto addStatus(Integer status) {
        return new StatusResponseDto(status);
    }

}
