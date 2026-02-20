package Zero_Knowledge_Vault.infra.security.jwt;


import Zero_Knowledge_Vault.domain.member.type.MemberRole;
import Zero_Knowledge_Vault.infra.security.AuthLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomUserPrincipal {
    private Long userId;
    private String email;
    private MemberRole role;
    private AuthLevel authLevel;
}

