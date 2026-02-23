package Zero_Knowledge_Vault.domain.member.dto;


import Zero_Knowledge_Vault.infra.security.jwt.CustomUserPrincipal;

public record MeResponseDto(
        Long userId,
        String email,
        String role,
        String authLevel,
        boolean pakeRegistered
) {

    public static MeResponseDto from(CustomUserPrincipal user, boolean pakeRegistered) {
        return new MeResponseDto(
                user.getUserId(),
                user.getEmail(),
                user.getRole().toString(),
                user.getAuthLevel().toString(),
                pakeRegistered
        );
    }
}
