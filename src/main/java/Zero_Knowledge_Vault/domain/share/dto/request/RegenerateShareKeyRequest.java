package Zero_Knowledge_Vault.domain.share.dto.request;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public record RegenerateShareKeyRequest(
        @Schema(description = "New RSA-OAEP public key encoded by the client as Base64")
        @NotBlank String publicKeyBase64,
        @Schema(description = "New client-encrypted private key ciphertext encoded as Base64. The server treats this as opaque data.")
        @NotBlank String encryptedPrivateKeyBase64,
        @Schema(description = "Share key algorithm. Currently only RSA-OAEP-256 is accepted.", example = "RSA-OAEP-256")
        @NotBlank String algorithm
) {
}
