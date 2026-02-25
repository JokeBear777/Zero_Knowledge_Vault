package Zero_Knowledge_Vault.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record PakeRegisterRequest(
        String verifier,   // hex string
        String saltAuth    // base64
) {}
