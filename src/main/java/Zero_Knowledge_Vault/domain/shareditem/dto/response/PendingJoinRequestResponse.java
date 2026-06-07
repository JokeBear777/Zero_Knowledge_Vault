package Zero_Knowledge_Vault.domain.shareditem.dto.response;

import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemJoinRequest;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemJoinRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record PendingJoinRequestResponse(
        Long joinRequestId,
        Long requesterMemberId,
        Integer requesterKeyVersion,
        @Schema(description = "Requester ACTIVE public share key. encryptedPrivateKeyBase64 is intentionally excluded.")
        String requesterPublicKeyBase64,
        String requesterEmailMasked,
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
