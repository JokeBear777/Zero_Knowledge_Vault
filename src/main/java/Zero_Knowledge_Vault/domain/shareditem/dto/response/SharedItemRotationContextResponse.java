package Zero_Knowledge_Vault.domain.shareditem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Current shared item rotation context for the owner client.")
public record SharedItemRotationContextResponse(
        @Schema(description = "Shared item id", example = "1")
        Long sharedItemId,
        @Schema(description = "Current shared item ciphertext version", example = "3")
        Long version,
        @Schema(description = "Current sharedItemKey version", example = "1")
        Long keyVersion,
        @Schema(description = "Current shared item membership/wrapper snapshot version", example = "2")
        Long membershipVersion,
        @Schema(description = "ACTIVE members that need a new encryptedItemKey wrapper")
        List<RotationMemberResponse> members
) {
}
