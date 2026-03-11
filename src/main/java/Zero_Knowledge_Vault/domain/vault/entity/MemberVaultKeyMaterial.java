package Zero_Knowledge_Vault.domain.vault.entity;

import Zero_Knowledge_Vault.domain.member.entity.Member;
import Zero_Knowledge_Vault.domain.vault.type.VaultKeyStatus;
import Zero_Knowledge_Vault.global.crypto.kdf.KdfAlgorithm;
import Zero_Knowledge_Vault.global.crypto.kdf.KdfParams;
import Zero_Knowledge_Vault.global.crypto.kdf.KdfParamsConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_vault_key_material")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberVaultKeyMaterial {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Lob
    @Column(name = "wrapped_vault_key", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] wrappedVaultKey;

    @Lob
    @Column(name = "salt_wrap", nullable = false, columnDefinition = "VARBINARY(32)")
    private byte[] saltWrap;

    @Enumerated(EnumType.STRING)
    @Column(name = "kdf_algorithm", nullable = false, length = 20)
    private KdfAlgorithm kdfAlgorithm;

    @Column(name = "kdf_params", columnDefinition = "json", nullable = false)
    @Convert(converter = KdfParamsConverter.class)
    private KdfParams kdfParams;

    @Column(name = "key_version", nullable = false)
    private Integer keyVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VaultKeyStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private MemberVaultKeyMaterial(
            Member member,
            byte[] wrappedVaultKey,
            byte[] saltWrap,
            KdfAlgorithm kdfAlgorithm,
            KdfParams kdfParams,
            Integer keyVersion,
            VaultKeyStatus status
    ) {
        this.member = member;
        this.wrappedVaultKey = wrappedVaultKey;
        this.saltWrap = saltWrap;
        this.kdfAlgorithm = kdfAlgorithm;
        this.kdfParams = kdfParams;
        this.keyVersion = keyVersion;
        this.status = status;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.keyVersion == null) {
            this.keyVersion = 1;
        }

        if (this.status == null) {
            this.status = VaultKeyStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static MemberVaultKeyMaterial register(
            Member member,
            byte[] wrappedVaultKey,
            byte[] saltWrap,
            KdfAlgorithm kdfAlgorithm,
            KdfParams kdfParams
    ) {
        return new MemberVaultKeyMaterial(
                member,
                wrappedVaultKey,
                saltWrap,
                kdfAlgorithm,
                kdfParams,
                1,
                VaultKeyStatus.ACTIVE
        );
    }

    public void rotate(
            byte[] newWrappedVaultKey,
            byte[] newSaltWrap,
            KdfAlgorithm newKdfAlgorithm,
            KdfParams newKdfParams
    ) {
        validateActive();

        this.wrappedVaultKey = newWrappedVaultKey;
        this.saltWrap = newSaltWrap;
        this.kdfAlgorithm = newKdfAlgorithm;
        this.kdfParams = newKdfParams;
        this.keyVersion += 1;
        this.status = VaultKeyStatus.ACTIVE;
    }

    public void markRotating() {
        validateNotDisabled();
        this.status = VaultKeyStatus.ROTATING;
    }

    public void disable() {
        this.status = VaultKeyStatus.DISABLED;
    }

    public boolean isActive() {
        return this.status == VaultKeyStatus.ACTIVE;
    }

    public boolean isDisabled() {
        return this.status == VaultKeyStatus.DISABLED;
    }

    private void validateActive() {
        if (this.status == VaultKeyStatus.DISABLED) {
            throw new IllegalStateException("Disabled vault key material cannot be rotated");
        }
    }

    private void validateNotDisabled() {
        if (this.status == VaultKeyStatus.DISABLED) {
            throw new IllegalStateException("Disabled vault key material cannot change state");
        }
    }
}
