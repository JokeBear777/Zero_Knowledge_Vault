package Zero_Knowledge_Vault.domain.shareditem.dto.response;

import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItem;
import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemMember;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemMemberRole;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemPermission;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Shared item response. Ciphertext and encryptedItemKeyBase64 are opaque to the server and decrypted only by the client.")
public record SharedItemResponse(
        @Schema(description = "Shared item id", example = "1")
        Long sharedItemId,
        @Schema(description = "Caller role for this shared item", example = "OWNER")
        SharedItemMemberRole role,
        @Schema(description = "Caller permission for this shared item", example = "READ_WRITE")
        SharedItemPermission permission,
        @Schema(description = "Shared item lifecycle status", example = "ACTIVE")
        SharedItemStatus status,
        @Schema(description = "Client-encrypted title ciphertext")
        String titleCipherBase64,
        @Schema(description = "Client-encrypted item ciphertext")
        String itemCipherBase64,
        @Schema(description = "Item cipher algorithm selected by the client", example = "AES-GCM-256")
        String itemCipherAlgorithm,
        @Schema(description = "sharedItemKey encrypted for the current member")
        String encryptedItemKeyBase64,
        @Schema(description = "Recipient share key version used for encryptedItemKeyBase64", example = "1")
        Integer recipientKeyVersion,
        @Schema(description = "Current shared item version for optimistic concurrency control", example = "3")
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SharedItemResponse from(SharedItemMember member) {
        SharedItem item = member.getSharedItem();
        return new SharedItemResponse(
                item.getSharedItemId(),
                member.getMemberRole(),
                member.getPermission(),
                item.getStatus(),
                item.getTitleCipherBase64(),
                item.getItemCipherBase64(),
                item.getItemCipherAlgorithm(),
                member.getEncryptedItemKeyBase64(),
                member.getRecipientKeyVersion(),
                item.getVersion(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
