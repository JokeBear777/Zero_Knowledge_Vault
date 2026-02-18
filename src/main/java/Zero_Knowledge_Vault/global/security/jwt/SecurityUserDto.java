package Zero_Knowledge_Vault.global.security.jwt;


import Zero_Knowledge_Vault.domain.member.type.MemberRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityUserDto {
    private Long userId;
    private String email;
    private String mobile;
    private MemberRole role;
}

