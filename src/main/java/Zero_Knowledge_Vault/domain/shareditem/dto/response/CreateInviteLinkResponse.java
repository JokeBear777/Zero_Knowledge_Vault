package Zero_Knowledge_Vault.domain.shareditem.dto.response;

import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemInviteLink;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemInviteLinkStatus;

import java.time.LocalDateTime;

public record CreateInviteLinkResponse(
        Long inviteLinkId,
        String inviteUrl,
        LocalDateTime expiresAt,
        SharedItemInviteLinkStatus status
) {
    public static CreateInviteLinkResponse from(SharedItemInviteLink link, String inviteUrl) {
        return new CreateInviteLinkResponse(
                link.getInviteLinkId(),
                inviteUrl,
                link.getExpiresAt(),
                link.getStatus()
        );
    }
}
