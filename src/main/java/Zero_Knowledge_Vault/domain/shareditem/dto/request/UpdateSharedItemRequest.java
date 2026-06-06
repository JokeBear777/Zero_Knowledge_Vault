package Zero_Knowledge_Vault.domain.shareditem.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateSharedItemRequest(
        @Schema(description = "Expected current shared item version for optimistic concurrency control")
        @NotNull Long expectedVersion,
        @Schema(description = "New client-encrypted shared item title ciphertext encoded as Base64")
        String titleCipherBase64,
        @Schema(description = "New client-encrypted shared item body ciphertext encoded as Base64")
        @NotBlank String itemCipherBase64
) {
}
