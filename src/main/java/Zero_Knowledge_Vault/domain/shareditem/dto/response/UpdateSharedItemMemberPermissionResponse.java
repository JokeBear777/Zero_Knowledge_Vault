package Zero_Knowledge_Vault.domain.shareditem.dto.response;

import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemMember;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemMemberRole;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemMemberStatus;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemPermission;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Shared item member permission update response.")
public record UpdateSharedItemMemberPermissionResponse(
        @Schema(description = "Shared item id", example = "1")
        Long sharedItemId,
        @Schema(description = "Participant member id", example = "2")
        Long memberId,
        @Schema(description = "Member role", example = "PARTICIPANT")
        SharedItemMemberRole role,
        @Schema(description = "Updated permission", example = "READ_WRITE")
        SharedItemPermission permission,
        @Schema(description = "Membership status", example = "ACTIVE")
        SharedItemMemberStatus status,
        LocalDateTime updatedAt
) {
    public static UpdateSharedItemMemberPermissionResponse from(SharedItemMember member, LocalDateTime updatedAt) {
        return new UpdateSharedItemMemberPermissionResponse(
                member.getSharedItem().getSharedItemId(),
                member.getMember().getMemberId(),
                member.getMemberRole(),
                member.getPermission(),
                member.getStatus(),
                updatedAt
        );
    }
}
