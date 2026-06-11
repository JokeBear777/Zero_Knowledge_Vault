package Zero_Knowledge_Vault.domain.shareditem.dto.response;

import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemJoinRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Owner decision response for a join request.")
public record JoinRequestDecisionResponse(
        @Schema(description = "Join request id", example = "10")
        Long joinRequestId,
        @Schema(description = "Shared item id", example = "1")
        Long sharedItemId,
        @Schema(description = "Decision status", example = "APPROVED")
        SharedItemJoinRequestStatus status,
        LocalDateTime decidedAt
) {
}
