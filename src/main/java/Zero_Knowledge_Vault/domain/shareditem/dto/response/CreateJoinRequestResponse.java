package Zero_Knowledge_Vault.domain.shareditem.dto.response;

import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemJoinRequest;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemJoinRequestStatus;

import java.time.LocalDateTime;

public record CreateJoinRequestResponse(
        Long joinRequestId,
        Long sharedItemId,
        SharedItemJoinRequestStatus status,
        LocalDateTime requestedAt
) {
    public static CreateJoinRequestResponse from(SharedItemJoinRequest request) {
        return new CreateJoinRequestResponse(
                request.getJoinRequestId(),
                request.getSharedItem().getSharedItemId(),
                request.getStatus(),
                request.getRequestedAt()
        );
    }
}
