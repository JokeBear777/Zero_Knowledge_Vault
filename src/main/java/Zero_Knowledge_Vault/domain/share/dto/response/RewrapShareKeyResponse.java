package Zero_Knowledge_Vault.domain.share.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Result of re-wrapping shared item wrappers with the caller's current ACTIVE share key.")
public record RewrapShareKeyResponse(
        @Schema(description = "Caller's ACTIVE share key version used for the rewrap", example = "2")
        Integer targetKeyVersion,
        @Schema(description = "Number of shared item wrappers updated", example = "3")
        Integer rewrappedItemCount,
        @Schema(description = "Server timestamp when the batch update completed")
        LocalDateTime rewrappedAt
) {
}
