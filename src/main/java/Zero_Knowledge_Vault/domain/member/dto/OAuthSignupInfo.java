package Zero_Knowledge_Vault.domain.member.dto;

public record OAuthSignupInfo(
        String email,
        String provider,
        String name,
        String mobile
) {
}
