package Zero_Knowledge_Vault.domain.auth.dto;

public record SrpAuthProveRequest(
        String authSessionId,
        String M1
) {}