package Zero_Knowledge_Vault.domain.shareditem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Shared item member revoke response.")
public record RevokeSharedItemMemberResponse(
        @Schema(description = "Shared item id", example = "1")
        Long sharedItemId,
        @Schema(description = "Revoked participant member id", example = "2")
        Long memberId,
        @Schema(description = "Whether the member was revoked", example = "true")
        boolean revoked
) {
}
