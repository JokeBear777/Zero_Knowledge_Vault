package Zero_Knowledge_Vault.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class RegisterAuthRequest {

    @NotBlank
    private String saltBase64;

    @NotBlank
    private String verifierBase64;
}
