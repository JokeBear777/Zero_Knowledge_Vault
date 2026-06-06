package Zero_Knowledge_Vault.domain.shareditem.dto.response;

import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemJoinRequestStatus;

import java.time.LocalDateTime;

public record JoinRequestDecisionResponse(
        Long joinRequestId,
        Long sharedItemId,
        SharedItemJoinRequestStatus status,
        LocalDateTime decidedAt
) {
}
