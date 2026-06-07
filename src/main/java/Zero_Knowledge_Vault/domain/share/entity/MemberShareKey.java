package Zero_Knowledge_Vault.domain.share.entity;

import Zero_Knowledge_Vault.domain.member.entity.Member;
import Zero_Knowledge_Vault.domain.share.type.ShareKeyStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "member_share_key",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_member_share_key_member_version",
                        columnNames = {"member_id", "key_version"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberShareKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "share_key_id", nullable = false)
    private Long shareKeyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "key_version", nullable = false)
    private Integer keyVersion;

    @Lob
    @Column(name = "public_key_base64", nullable = false, columnDefinition = "TEXT")
    private String publicKeyBase64;

    @Lob
    @Column(name = "encrypted_private_key_base64", nullable = false, columnDefinition = "TEXT")
    private String encryptedPrivateKeyBase64;

    @Column(name = "algorithm", nullable = false, length = 50)
    private String algorithm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShareKeyStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "rotated_at")
    private LocalDateTime rotatedAt;

    private MemberShareKey(
            Member member,
            Integer keyVersion,
            String publicKeyBase64,
            String encryptedPrivateKeyBase64,
            String algorithm
    ) {
        this.member = member;
        this.keyVersion = keyVersion;
        this.publicKeyBase64 = publicKeyBase64;
        this.encryptedPrivateKeyBase64 = encryptedPrivateKeyBase64;
        this.algorithm = algorithm;
        this.status = ShareKeyStatus.ACTIVE;
    }

    public static MemberShareKey create(
            Member member,
            Integer keyVersion,
            String publicKeyBase64,
            String encryptedPrivateKeyBase64,
            String algorithm
    ) {
        return new MemberShareKey(
                member,
                keyVersion,
                publicKeyBase64,
                encryptedPrivateKeyBase64,
                algorithm
        );
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = ShareKeyStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.status = ShareKeyStatus.DELETED;
    }

    public void rotate(LocalDateTime rotatedAt) {
        this.status = ShareKeyStatus.ROTATED;
        this.rotatedAt = rotatedAt;
    }
}
