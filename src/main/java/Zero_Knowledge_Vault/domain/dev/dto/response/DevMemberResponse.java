package Zero_Knowledge_Vault.domain.dev.dto.response;

import Zero_Knowledge_Vault.domain.member.entity.Member;
import Zero_Knowledge_Vault.domain.member.type.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Local profile only test member summary.")
public record DevMemberResponse(
        @Schema(description = "Member id", example = "1")
        Long memberId,
        @Schema(description = "Member email", example = "owner@test.com")
        String email,
        @Schema(description = "Member nickname/name", example = "Owner")
        String nickname,
        @Schema(description = "Member role", example = "ROLE_USER")
        MemberRole memberRole
) {
    public static DevMemberResponse from(Member member) {
        return new DevMemberResponse(
                member.getMemberId(),
                member.getEmail(),
                member.getName(),
                member.getMemberRole()
        );
    }
}
