package Zero_Knowledge_Vault.domain.shareditem.dto.response;

import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemInviteLink;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemInviteLinkStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Invite link response. Raw invite token is returned once inside inviteUrl; only token hash is stored.")
public record CreateInviteLinkResponse(
        @Schema(description = "Invite link id", example = "100")
        Long inviteLinkId,
        @Schema(description = "Frontend invite URL containing the raw one-time token")
        String inviteUrl,
        LocalDateTime expiresAt,
        @Schema(description = "Invite link status", example = "ACTIVE")
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
