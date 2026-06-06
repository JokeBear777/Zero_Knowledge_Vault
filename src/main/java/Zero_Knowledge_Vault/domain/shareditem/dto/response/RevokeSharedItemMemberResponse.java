package Zero_Knowledge_Vault.domain.shareditem.dto.response;

public record RevokeSharedItemMemberResponse(
        Long sharedItemId,
        Long memberId,
        boolean revoked
) {
}
