package Zero_Knowledge_Vault.domain.shareditem.dto.response;

import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemJoinRequest;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemJoinRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Pending join request visible to the owner. Includes requester public key for client-side sharedItemKey wrapping.")
public record PendingJoinRequestResponse(
        @Schema(description = "Join request id", example = "10")
        Long joinRequestId,
        @Schema(description = "Requester member id", example = "2")
        Long requesterMemberId,
        @Schema(description = "Requester ACTIVE share key version", example = "1")
        Integer requesterKeyVersion,
        @Schema(description = "Requester ACTIVE public share key. encryptedPrivateKeyBase64 is intentionally excluded.")
        String requesterPublicKeyBase64,
        @Schema(description = "Masked requester email for owner UI")
        String requesterEmailMasked,
        @Schema(description = "Join request status", example = "PENDING")
        SharedItemJoinRequestStatus status,
        LocalDateTime requestedAt
) {
    public static PendingJoinRequestResponse from(
            SharedItemJoinRequest request,
            String requesterPublicKeyBase64,
            String requesterEmailMasked
    ) {
        return new PendingJoinRequestResponse(
                request.getJoinRequestId(),
                request.getRequester().getMemberId(),
                request.getRequesterKeyVersion(),
                requesterPublicKeyBase64,
                requesterEmailMasked,
                request.getStatus(),
                request.getRequestedAt()
        );
    }
}
