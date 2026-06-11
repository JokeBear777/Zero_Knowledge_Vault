package Zero_Knowledge_Vault.domain.shareditem.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to create a shared secret. All title/content/key fields are already encrypted by the client.")
public record CreateSharedItemRequest(
        @Schema(description = "Client-encrypted shared item title ciphertext encoded as Base64", example = "base64-title-cipher")
        String titleCipherBase64,
        @Schema(description = "Client-encrypted shared item body ciphertext encoded as Base64", example = "base64-item-cipher")
        @NotBlank String itemCipherBase64,
        @Schema(description = "Item cipher algorithm", example = "AES-GCM-256")
        @NotBlank String itemCipherAlgorithm,
        @Schema(description = "sharedItemKey encrypted for the owner public key, encoded as Base64", example = "base64-owner-encrypted-item-key")
        @NotBlank String ownerEncryptedItemKeyBase64,
        @Schema(description = "Owner ACTIVE share key version used for ownerEncryptedItemKeyBase64", example = "1")
        @NotNull Integer ownerKeyVersion
) {
}
