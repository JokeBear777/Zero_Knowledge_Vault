package Zero_Knowledge_Vault.domain.vault.dto;

import Zero_Knowledge_Vault.domain.vault.entity.VaultItem;

import java.util.Base64;

public record GetVaultItemResponse(
        String itemId,
        String itemKeyCipherBase64,
        String itemCipherBase64,
        long version
) {

    public static GetVaultItemResponse fromEntity(VaultItem item) {
        String itemKeyCipherBase64 = Base64.getEncoder().encodeToString(item.getItemKeyCipher());
        String itemCipherBase64 = Base64.getEncoder().encodeToString(item.getItemCipher());

        return new GetVaultItemResponse(
                item.getItemId(),
                itemKeyCipherBase64,
                itemCipherBase64,
                item.getVersion()
        );
    }

}
