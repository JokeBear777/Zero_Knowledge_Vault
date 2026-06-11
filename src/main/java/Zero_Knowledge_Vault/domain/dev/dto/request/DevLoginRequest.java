package Zero_Knowledge_Vault.domain.dev.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Local profile only dev login request. Creates or reuses a test member.")
public record DevLoginRequest(
        @Schema(description = "Test member email", example = "owner@test.com")
        @Email @NotBlank String email,
        @Schema(description = "Test member nickname/name", example = "Owner")
        @NotBlank String nickname
) {
}
