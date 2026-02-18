package Zero_Knowledge_Vault.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequestDto {

    @NotBlank(message = "이름은 필수 입니다.")
    @Size(max = 255, message = "이름은 최대 255자까지 허용됩니다.")
    private String name;

    @NotBlank(message = "휴대폰 번호는 필수 입니다.")
    @Size(max = 255, message = "휴대폰 번호는 최대 255자까지 허용됩니다.")
    private String mobile;
}
