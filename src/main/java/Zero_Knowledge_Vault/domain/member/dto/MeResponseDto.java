package Zero_Knowledge_Vault.domain.member.dto;


import Zero_Knowledge_Vault.global.security.jwt.SecurityUserDto;

public record MeResponseDto(
        Long userId,
        String email,
        String role,
        String authLevel
) {

    public static MeResponseDto from(SecurityUserDto user) {
        return new MeResponseDto(
                user.getUserId(),
                user.getEmail(),
                user.getRole().toString(),
                user.getAuthLevel().toString()
        );
    }
}
