package Zero_Knowledge_Vault.domain.dev.dto.response;

import Zero_Knowledge_Vault.domain.member.entity.Member;
import Zero_Knowledge_Vault.infra.security.AuthLevel;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Local profile only dev auth response. accessToken is intended for Swagger Bearer authorization.")
public record DevAuthResponse(
        @Schema(description = "Member id", example = "1")
        Long memberId,
        @Schema(description = "Member email", example = "owner@test.com")
        String email,
        @Schema(description = "Member nickname/name", example = "Owner")
        String nickname,
        @Schema(description = "JWT auth level", example = "VAULT_AUTH")
        AuthLevel authLevel,
        @Schema(description = "JWT access token for local Swagger/API testing")
        String accessToken
) {
    public static DevAuthResponse of(Member member, AuthLevel authLevel, String accessToken) {
        return new DevAuthResponse(
                member.getMemberId(),
                member.getEmail(),
                member.getName(),
                authLevel,
                accessToken
        );
    }
}
