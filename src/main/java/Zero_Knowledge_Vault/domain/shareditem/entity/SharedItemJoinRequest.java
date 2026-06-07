package Zero_Knowledge_Vault.domain.shareditem.entity;

import Zero_Knowledge_Vault.domain.member.entity.Member;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemJoinRequestStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "shared_item_join_request",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_shared_item_join_request_unique",
                columnNames = {"shared_item_id", "requester_member_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SharedItemJoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "join_request_id", nullable = false)
    private Long joinRequestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "shared_item_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_shared_item_join_request_shared_item")
    )
    private SharedItem sharedItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "invite_link_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_shared_item_join_request_invite_link")
    )
    private SharedItemInviteLink inviteLink;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "requester_member_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_shared_item_join_request_requester")
    )
    private Member requester;

    @Column(name = "requester_key_version", nullable = false)
    private Integer requesterKeyVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SharedItemJoinRequestStatus status;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "decided_by_member_id",
            foreignKey = @ForeignKey(name = "fk_shared_item_join_request_decider")
    )
    private Member decidedBy;

    private SharedItemJoinRequest(
            SharedItem sharedItem,
            SharedItemInviteLink inviteLink,
            Member requester,
            Integer requesterKeyVersion
    ) {
        this.sharedItem = sharedItem;
        this.inviteLink = inviteLink;
        this.requester = requester;
        this.requesterKeyVersion = requesterKeyVersion;
        this.status = SharedItemJoinRequestStatus.PENDING;
    }

    public static SharedItemJoinRequest create(
            SharedItem sharedItem,
            SharedItemInviteLink inviteLink,
            Member requester,
            Integer requesterKeyVersion
    ) {
        return new SharedItemJoinRequest(sharedItem, inviteLink, requester, requesterKeyVersion);
    }

    @PrePersist
    protected void onCreate() {
        if (this.requestedAt == null) {
            this.requestedAt = LocalDateTime.now();
        }

        if (this.status == null) {
            this.status = SharedItemJoinRequestStatus.PENDING;
        }
    }
}
