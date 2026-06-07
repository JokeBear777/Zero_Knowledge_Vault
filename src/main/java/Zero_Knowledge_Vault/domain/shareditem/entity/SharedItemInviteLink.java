package Zero_Knowledge_Vault.domain.shareditem.entity;

import Zero_Knowledge_Vault.domain.member.entity.Member;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemInviteLinkStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "shared_item_invite_link",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_shared_item_invite_token_hash",
                columnNames = "invite_token_hash"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SharedItemInviteLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invite_link_id", nullable = false)
    private Long inviteLinkId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "shared_item_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_shared_item_invite_link_shared_item")
    )
    private SharedItem sharedItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "owner_member_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_shared_item_invite_link_owner")
    )
    private Member owner;

    @Column(name = "invite_token_hash", nullable = false, length = 128)
    private String inviteTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SharedItemInviteLinkStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    private SharedItemInviteLink(
            SharedItem sharedItem,
            Member owner,
            String inviteTokenHash,
            LocalDateTime expiresAt
    ) {
        this.sharedItem = sharedItem;
        this.owner = owner;
        this.inviteTokenHash = inviteTokenHash;
        this.status = SharedItemInviteLinkStatus.ACTIVE;
        this.expiresAt = expiresAt;
    }

    public static SharedItemInviteLink create(
            SharedItem sharedItem,
            Member owner,
            String inviteTokenHash,
            LocalDateTime expiresAt
    ) {
        return new SharedItemInviteLink(sharedItem, owner, inviteTokenHash, expiresAt);
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }

        if (this.status == null) {
            this.status = SharedItemInviteLinkStatus.ACTIVE;
        }
    }
}
