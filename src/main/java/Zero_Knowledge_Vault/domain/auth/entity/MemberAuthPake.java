package Zero_Knowledge_Vault.domain.auth.entity;

import Zero_Knowledge_Vault.domain.auth.converter.KdfParamsConverter;
import Zero_Knowledge_Vault.domain.auth.type.AuthStatus;
import Zero_Knowledge_Vault.domain.auth.type.KdfAlgorithm;
import Zero_Knowledge_Vault.domain.auth.vo.KdfParams;
import Zero_Knowledge_Vault.domain.auth.type.PakeAlgorithm;
import Zero_Knowledge_Vault.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "member_auth_pake")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MemberAuthPake {

    /**
     * PK = FK 구조 (Member와 1:1 공유키)
     */
    @Id
    @Column(name = "member_id")
    private Long memberId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * PAKE 알고리즘 종류 (SRP-6a, OPAQUE 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "algo", nullable = false, length = 20)
    private PakeAlgorithm algorithm;

    /**
     * SRP 그룹 파라미터 식별자 (N,g 세트 버전)
     */
    @Column(name = "group_id", nullable = false, length = 50)
    private String groupId;

    /**
     * 인증용 salt
     */
    @Lob
    @Column(name = "salt_auth", nullable = false)
    private byte[] saltAuth;

    /**
     * SRP verifier (g^x mod N)
     */
    @Lob
    @Column(name = "verifier", nullable = false)
    private byte[] verifier;

    /**
     * KDF 종류
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "kdf_auth", nullable = false, length = 20)
    private KdfAlgorithm kdfAlgorithm;

    /**
     * KDF 파라미터 (JSON 저장)
     */
    @Column(name = "kdf_params_auth", columnDefinition = "json", nullable = false)
    @Convert(converter = KdfParamsConverter.class)
    private KdfParams kdfParams;

    /**
     * 상태 (ACTIVE / ROTATING / DISABLED)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AuthStatus status;

    /**
     * 프로토콜/스키마 버전
     */
    @Column(name = "auth_version", nullable = false)
    private Integer authVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.authVersion == null) {
            this.authVersion = 1;
        }
        if (this.status == null) {
            this.status = AuthStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
