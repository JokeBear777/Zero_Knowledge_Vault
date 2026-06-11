package Zero_Knowledge_Vault.domain.shareditem.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "New sharedItemKey wrapper for one remaining ACTIVE shared item member.")
public record MemberKeyWrapperRequest(
        @Schema(description = "Recipient member id", example = "2")
        @NotNull Long memberId,
        @Schema(description = "Recipient ACTIVE public share key version used for this wrapper", example = "1")
        @NotNull Integer recipientKeyVersion,
        @Schema(description = "New sharedItemKey encrypted for the recipient public key, encoded as Base64")
        @NotBlank String encryptedItemKeyBase64
) {
}
