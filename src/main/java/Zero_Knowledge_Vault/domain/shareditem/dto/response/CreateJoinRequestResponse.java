package Zero_Knowledge_Vault.domain.shareditem.dto.response;

import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemJoinRequest;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemJoinRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Join request creation response. Access is not granted until owner approval.")
public record CreateJoinRequestResponse(
        @Schema(description = "Join request id", example = "10")
        Long joinRequestId,
        @Schema(description = "Shared item id", example = "1")
        Long sharedItemId,
        @Schema(description = "Join request status", example = "PENDING")
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
