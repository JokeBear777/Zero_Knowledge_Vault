package Zero_Knowledge_Vault.domain.share.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RewrapItemRequest(
        @Schema(description = "Shared item id whose wrapper should be re-wrapped", example = "10")
        @NotNull
        Long sharedItemId,
        @Schema(description = "Expected shared item membershipVersion for optimistic concurrency control", example = "2")
        @NotNull
        Long expectedMembershipVersion,
        @Schema(description = "Client-rewrapped sharedItemKey ciphertext for the current user's ACTIVE share key", example = "YWJjMTIz")
        @NotBlank
        String encryptedItemKeyBase64
) {
}
