package Zero_Knowledge_Vault.domain.shareditem.dto.response;

import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItem;
import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemMember;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemMemberRole;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemPermission;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record SharedItemResponse(
        Long sharedItemId,
        SharedItemMemberRole role,
        SharedItemPermission permission,
        SharedItemStatus status,
        @Schema(description = "Client-encrypted title ciphertext")
        String titleCipherBase64,
        @Schema(description = "Client-encrypted item ciphertext")
        String itemCipherBase64,
        String itemCipherAlgorithm,
        @Schema(description = "Shared item key encrypted for the current member")
        String encryptedItemKeyBase64,
        Integer recipientKeyVersion,
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
