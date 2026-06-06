package Zero_Knowledge_Vault.domain.shareditem.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSharedItemRequest(
        @Schema(description = "Client-encrypted shared item title ciphertext encoded as Base64")
        String titleCipherBase64,
        @Schema(description = "Client-encrypted shared item body ciphertext encoded as Base64")
        @NotBlank String itemCipherBase64,
        @Schema(description = "Item cipher algorithm", example = "AES-GCM-256")
        @NotBlank String itemCipherAlgorithm,
        @Schema(description = "Shared item key encrypted for the owner, encoded as Base64")
        @NotBlank String ownerEncryptedItemKeyBase64,
        @Schema(description = "Owner ACTIVE share key version used for ownerEncryptedItemKeyBase64")
        @NotNull Integer ownerKeyVersion
) {
}
