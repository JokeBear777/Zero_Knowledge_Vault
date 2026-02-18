package Zero_Knowledge_Vault.global.util.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
public class BaseResponseDto {
    protected Integer status;
    protected Object data;

    public BaseResponseDto(Integer status) {
        this.status = status;
    }

}
