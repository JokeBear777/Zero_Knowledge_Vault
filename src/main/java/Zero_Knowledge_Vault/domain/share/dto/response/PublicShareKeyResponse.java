package Zero_Knowledge_Vault.domain.share.dto.response;

import Zero_Knowledge_Vault.domain.share.entity.MemberShareKey;
import Zero_Knowledge_Vault.domain.share.type.ShareKeyStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record PublicShareKeyResponse(
        Long memberId,
        Integer keyVersion,
        @Schema(description = "Target member public key encoded as Base64. encryptedPrivateKeyBase64 is intentionally excluded.")
        String publicKeyBase64,
        String algorithm,
        ShareKeyStatus status
) {
    public static PublicShareKeyResponse from(MemberShareKey key) {
        return new PublicShareKeyResponse(
                key.getMember().getMemberId(),
                key.getKeyVersion(),
                key.getPublicKeyBase64(),
                key.getAlgorithm(),
                key.getStatus()
        );
    }
}
