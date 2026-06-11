package Zero_Knowledge_Vault.domain.shareditem.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Owner key rotation payload. All ciphertext and key wrappers are produced client-side.")
public record RotateSharedItemKeyRequest(
        @Schema(description = "Expected current shared item ciphertext version", example = "3")
        @NotNull Long expectedVersion,
        @Schema(description = "Expected current sharedItemKey version", example = "1")
        @NotNull Long expectedKeyVersion,
        @Schema(description = "Expected current shared item membership/wrapper snapshot version", example = "2")
        @NotNull Long expectedMembershipVersion,
        @Schema(description = "Title ciphertext re-encrypted by the client with the new sharedItemKey, encoded as Base64")
        @NotBlank String titleCipherBase64,
        @Schema(description = "Item ciphertext re-encrypted by the client with the new sharedItemKey, encoded as Base64")
        @NotBlank String itemCipherBase64,
        @Schema(description = "Wrappers for every remaining ACTIVE member after revokedMemberIds are removed")
        @NotNull @Size(min = 1) List<@Valid MemberKeyWrapperRequest> memberKeyWrappers,
        @Schema(description = "Member ids to revoke in the same transaction. Null is treated as an empty list.")
        List<Long> revokedMemberIds
) {
}
