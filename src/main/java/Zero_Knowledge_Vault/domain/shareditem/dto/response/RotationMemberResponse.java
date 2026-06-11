package Zero_Knowledge_Vault.domain.shareditem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "ACTIVE shared item member data needed by the owner client to build a rotation wrapper.")
public record RotationMemberResponse(
        @Schema(description = "Member id", example = "2")
        Long memberId,
        @Schema(description = "Member role", example = "PARTICIPANT")
        String role,
        @Schema(description = "Member permission", example = "READ_WRITE")
        String permission,
        @Schema(description = "Membership status", example = "ACTIVE")
        String status,
        @Schema(description = "Recipient ACTIVE public share key encoded as Base64")
        String publicKeyBase64,
        @Schema(description = "Recipient ACTIVE public share key version", example = "1")
        Integer publicKeyVersion
) {
}
