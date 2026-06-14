package Zero_Knowledge_Vault.domain.share.dto.response;

import Zero_Knowledge_Vault.domain.share.entity.MemberShareKey;
import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemMember;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemMemberRole;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemPermission;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Shared item wrapper candidate that can be re-wrapped to the caller's current ACTIVE share key.")
public record RewrapCandidateResponse(
        @Schema(description = "Shared item id", example = "10")
        Long sharedItemId,
        @Schema(description = "Shared item title ciphertext encoded as Base64")
        String titleCipherBase64,
        @Schema(description = "Current wrapper ciphertext encoded as Base64")
        String encryptedItemKeyBase64,
        @Schema(description = "Recipient share key version used by the current wrapper", example = "1")
        Integer recipientKeyVersion,
        @Schema(description = "Caller's current ACTIVE share key version", example = "2")
        Integer currentActiveKeyVersion,
        @Schema(description = "Current shared item version", example = "3")
        Long sharedItemVersion,
        @Schema(description = "Current shared item keyVersion", example = "1")
        Long keyVersion,
        @Schema(description = "Current shared item membershipVersion", example = "2")
        Long membershipVersion,
        @Schema(description = "Caller role for this shared item", example = "PARTICIPANT")
        SharedItemMemberRole role,
        @Schema(description = "Caller permission for this shared item", example = "READ_WRITE")
        SharedItemPermission permission
) {
    public static RewrapCandidateResponse from(
            SharedItemMember member,
            MemberShareKey activeShareKey
    ) {
        return new RewrapCandidateResponse(
                member.getSharedItem().getSharedItemId(),
                member.getSharedItem().getTitleCipherBase64(),
                member.getEncryptedItemKeyBase64(),
                member.getRecipientKeyVersion(),
                activeShareKey.getKeyVersion(),
                member.getSharedItem().getVersion(),
                member.getSharedItem().getKeyVersion(),
                member.getSharedItem().getMembershipVersion(),
                member.getMemberRole(),
                member.getPermission()
        );
    }
}
