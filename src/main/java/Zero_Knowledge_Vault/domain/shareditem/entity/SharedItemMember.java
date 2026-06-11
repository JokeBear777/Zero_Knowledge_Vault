package Zero_Knowledge_Vault.domain.shareditem.entity;

import Zero_Knowledge_Vault.domain.member.entity.Member;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemMemberRole;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemMemberStatus;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemPermission;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "shared_item_member",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_shared_item_member_unique",
                columnNames = {"shared_item_id", "member_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SharedItemMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shared_item_member_id", nullable = false)
    private Long sharedItemMemberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "shared_item_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_shared_item_member_shared_item")
    )
    private SharedItem sharedItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "member_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_shared_item_member_member")
    )
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 20)
    private SharedItemMemberRole memberRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 20)
    private SharedItemPermission permission;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SharedItemMemberStatus status;

    @Column(name = "recipient_key_version", nullable = false)
    private Integer recipientKeyVersion;

    @Lob
    @Column(name = "encrypted_item_key_base64", columnDefinition = "TEXT")
    private String encryptedItemKeyBase64;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    private SharedItemMember(
            SharedItem sharedItem,
            Member member,
            SharedItemMemberRole memberRole,
            SharedItemPermission permission,
            Integer recipientKeyVersion,
            String encryptedItemKeyBase64,
            LocalDateTime acceptedAt
    ) {
        this.sharedItem = sharedItem;
        this.member = member;
        this.memberRole = memberRole;
        this.permission = permission;
        this.status = SharedItemMemberStatus.ACTIVE;
        this.recipientKeyVersion = recipientKeyVersion;
        this.encryptedItemKeyBase64 = encryptedItemKeyBase64;
        this.acceptedAt = acceptedAt;
    }

    public static SharedItemMember owner(
            SharedItem sharedItem,
            Member member,
            Integer recipientKeyVersion,
            String encryptedItemKeyBase64,
            LocalDateTime now
    ) {
        return new SharedItemMember(
                sharedItem,
                member,
                SharedItemMemberRole.OWNER,
                SharedItemPermission.READ_WRITE,
                recipientKeyVersion,
                encryptedItemKeyBase64,
                now
        );
    }

    public static SharedItemMember participant(
            SharedItem sharedItem,
            Member member,
            SharedItemPermission permission,
            Integer recipientKeyVersion,
            String encryptedItemKeyBase64,
            LocalDateTime now
    ) {
        return new SharedItemMember(
                sharedItem,
                member,
                SharedItemMemberRole.PARTICIPANT,
                permission,
                recipientKeyVersion,
                encryptedItemKeyBase64,
                now
        );
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }

        if (this.updatedAt == null) {
            this.updatedAt = now;
        }

        if (this.status == null) {
            this.status = SharedItemMemberStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePermission(SharedItemPermission permission) {
        this.permission = permission;
    }

    public void revoke(LocalDateTime now) {
        this.status = SharedItemMemberStatus.REVOKED;
        this.encryptedItemKeyBase64 = null;
        this.revokedAt = now;
        this.updatedAt = now;
    }
}
