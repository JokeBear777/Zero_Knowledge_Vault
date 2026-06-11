package Zero_Knowledge_Vault.domain.shareditem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Shared item ciphertext update response.")
public record UpdateSharedItemResponse(
        @Schema(description = "Shared item id", example = "1")
        Long sharedItemId,
        @Schema(description = "New shared item version", example = "4")
        Long version,
        LocalDateTime updatedAt
) {
}
