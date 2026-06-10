package Zero_Knowledge_Vault.domain.shareditem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Shared item soft-delete response.")
public record DeleteSharedItemResponse(
        @Schema(description = "Whether the shared item was marked deleted", example = "true")
        boolean deleted
) {
}
