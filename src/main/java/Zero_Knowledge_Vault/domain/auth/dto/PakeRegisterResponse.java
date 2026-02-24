package Zero_Knowledge_Vault.domain.auth.dto;

import Zero_Knowledge_Vault.domain.auth.type.PakeAuthStatus;

public record PakeRegisterResponse(
        PakeAuthStatus status,
        Integer authVersion
) {
}
