package Zero_Knowledge_Vault.domain.vault.share.dto.response;

import Zero_Knowledge_Vault.domain.vault.share.entity.MemberShareKey;
import Zero_Knowledge_Vault.domain.vault.share.type.ShareKeyStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record MyShareKeyResponse(
        @Schema(description = "Whether the caller currently has an ACTIVE share key")
        boolean exists,
        Long shareKeyId,
        Integer keyVersion,
        @Schema(description = "Caller public key encoded as Base64")
        String publicKeyBase64,
        @Schema(description = "Client-encrypted private key ciphertext. This is not privateKey plaintext.")
        String encryptedPrivateKeyBase64,
        String algorithm,
        ShareKeyStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime rotatedAt
) {
    public static MyShareKeyResponse empty() {
        return new MyShareKeyResponse(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static MyShareKeyResponse from(MemberShareKey key) {
        return new MyShareKeyResponse(
                true,
                key.getShareKeyId(),
                key.getKeyVersion(),
                key.getPublicKeyBase64(),
                key.getEncryptedPrivateKeyBase64(),
                key.getAlgorithm(),
                key.getStatus(),
                key.getCreatedAt(),
                key.getUpdatedAt(),
                key.getRotatedAt()
        );
    }
}
