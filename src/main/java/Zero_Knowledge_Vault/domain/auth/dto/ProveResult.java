package Zero_Knowledge_Vault.domain.auth.dto;

public record ProveResult(
        String elevationToken,
        String M2Hex,
        long expiresInSeconds
) {}
