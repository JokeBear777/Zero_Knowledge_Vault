package Zero_Knowledge_Vault.domain.shareditem.dto.response;

import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemMember;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemMemberRole;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemMemberStatus;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemPermission;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Shared item member metadata. Does not include encryptedItemKeyBase64 or private key material.")
public record SharedItemMemberResponse(
        @Schema(description = "Member id", example = "2")
        Long memberId,
        @Schema(description = "Masked member email")
        String emailMasked,
        @Schema(description = "Member role", example = "PARTICIPANT")
        SharedItemMemberRole role,
        @Schema(description = "Member permission", example = "READ_ONLY")
        SharedItemPermission permission,
        @Schema(description = "Membership status", example = "ACTIVE")
        SharedItemMemberStatus status,
        @Schema(description = "Recipient share key version used for the stored encryptedItemKeyBase64", example = "1")
        Integer recipientKeyVersion,
        LocalDateTime createdAt,
        LocalDateTime acceptedAt,
        LocalDateTime revokedAt
) {
    public static SharedItemMemberResponse from(SharedItemMember member, String emailMasked) {
        return new SharedItemMemberResponse(
                member.getMember().getMemberId(),
                emailMasked,
                member.getMemberRole(),
                member.getPermission(),
                member.getStatus(),
                member.getRecipientKeyVersion(),
                member.getCreatedAt(),
                member.getAcceptedAt(),
                member.getRevokedAt()
        );
    }
}
