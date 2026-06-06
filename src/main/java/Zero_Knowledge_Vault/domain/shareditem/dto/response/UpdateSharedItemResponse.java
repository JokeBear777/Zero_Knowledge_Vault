package Zero_Knowledge_Vault.domain.shareditem.dto.response;

import java.time.LocalDateTime;

public record UpdateSharedItemResponse(
        Long sharedItemId,
        Long version,
        LocalDateTime updatedAt
) {
}
