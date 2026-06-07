package Zero_Knowledge_Vault.domain.shareditem.dto.response;

import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemMember;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemMemberRole;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemMemberStatus;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemPermission;

import java.time.LocalDateTime;

public record UpdateSharedItemMemberPermissionResponse(
        Long sharedItemId,
        Long memberId,
        SharedItemMemberRole role,
        SharedItemPermission permission,
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
