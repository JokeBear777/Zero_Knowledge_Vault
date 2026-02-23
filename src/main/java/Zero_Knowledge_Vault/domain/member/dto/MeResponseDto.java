package Zero_Knowledge_Vault.domain.member.dto;


import Zero_Knowledge_Vault.domain.auth.type.PakeAuthStatus;
import Zero_Knowledge_Vault.infra.security.jwt.CustomUserPrincipal;

public record MeResponseDto(
        Long userId,
        String email,
        String role,
        String authLevel,
        PakeAuthStatus pakeAuthStatus
) {

    public static MeResponseDto from(CustomUserPrincipal user,  PakeAuthStatus pakeAuthStatus) {
        return new MeResponseDto(
                user.getUserId(),
                user.getEmail(),
                user.getRole().toString(),
                user.getAuthLevel().toString(),
                pakeAuthStatus
        );
    }
}
