package Zero_Knowledge_Vault.domain.shareditem.dto.request;

import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemPermission;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApproveJoinRequestRequest(
        @Schema(description = "Requester ACTIVE share key version used for encryptedItemKeyBase64")
        @NotNull Integer recipientKeyVersion,
        @Schema(description = "Shared item key encrypted for the requester, encoded as Base64")
        @NotBlank String encryptedItemKeyBase64,
        @Schema(description = "Permission granted to the requester", example = "READ_ONLY")
        SharedItemPermission permission
) {
    public SharedItemPermission normalizedPermission() {
        return permission == null ? SharedItemPermission.READ_ONLY : permission;
    }
}
