package Zero_Knowledge_Vault.domain.shareditem.dto.response;

import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemMember;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemMemberRole;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemMemberStatus;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemPermission;

import java.time.LocalDateTime;

public record SharedItemMemberResponse(
        Long memberId,
        String emailMasked,
        SharedItemMemberRole role,
        SharedItemPermission permission,
        SharedItemMemberStatus status,
        Integer recipientKeyVersion,
        LocalDateTime createdAt,
        LocalDateTime acceptedAt,
        LocalDateTime revokedAt
) {
    public static SharedItemMemberResponse from(SharedItemMember member, String emailMasked) {
        return new SharedItemMemberResponse(
                member.getMember().getMemberId(),
                emailMasked,
                member.getMemberRole(),
                member.getPermission(),
                member.getStatus(),
                member.getRecipientKeyVersion(),
                member.getCreatedAt(),
                member.getAcceptedAt(),
                member.getRevokedAt()
        );
    }
}
