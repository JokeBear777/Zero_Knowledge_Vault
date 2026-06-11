package Zero_Knowledge_Vault.domain.shareditem.entity;

import Zero_Knowledge_Vault.domain.member.entity.Member;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "shared_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SharedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shared_item_id", nullable = false)
    private Long sharedItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "owner_member_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_shared_item_owner")
    )
    private Member owner;

    @Lob
    @Column(name = "title_cipher_base64", columnDefinition = "TEXT")
    private String titleCipherBase64;

    @Lob
    @Column(name = "item_cipher_base64", nullable = false, columnDefinition = "LONGTEXT")
    private String itemCipherBase64;

    @Column(name = "item_cipher_algorithm", nullable = false, length = 50)
    private String itemCipherAlgorithm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SharedItemStatus status;

    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "key_version", nullable = false)
    private Long keyVersion;

    @Column(name = "membership_version", nullable = false)
    private Long membershipVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private SharedItem(
            Member owner,
            String titleCipherBase64,
            String itemCipherBase64,
            String itemCipherAlgorithm
    ) {
        this.owner = owner;
        this.titleCipherBase64 = titleCipherBase64;
        this.itemCipherBase64 = itemCipherBase64;
        this.itemCipherAlgorithm = itemCipherAlgorithm;
        this.status = SharedItemStatus.ACTIVE;
        this.version = 1L;
        this.keyVersion = 1L;
        this.membershipVersion = 1L;
    }

    public static SharedItem create(
            Member owner,
            String titleCipherBase64,
            String itemCipherBase64,
            String itemCipherAlgorithm
    ) {
        return new SharedItem(owner, titleCipherBase64, itemCipherBase64, itemCipherAlgorithm);
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = SharedItemStatus.ACTIVE;
        }

        if (this.version == null) {
            this.version = 1L;
        }

        if (this.keyVersion == null) {
            this.keyVersion = 1L;
        }

        if (this.membershipVersion == null) {
            this.membershipVersion = 1L;
        }
    }

    public void markDeleted(LocalDateTime now) {
        this.status = SharedItemStatus.DELETED;
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public void increaseMembershipVersion(LocalDateTime now) {
        if (this.membershipVersion == null) {
            this.membershipVersion = 1L;
        }
        this.membershipVersion += 1;
        this.updatedAt = now;
    }
}
