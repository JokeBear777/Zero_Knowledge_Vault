package Zero_Knowledge_Vault.domain.shareditem.dto.request;

import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemPermission;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update participant permission metadata.")
public record UpdateSharedItemMemberPermissionRequest(
        @Schema(description = "Updated participant permission", example = "READ_WRITE")
        @NotNull SharedItemPermission permission
) {
}
