package Zero_Knowledge_Vault.domain.vault.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vault_index")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VaultIndex {

    //Todo 나중에 직접 생성하며 인덱스 붙여줘야함

    @Id
    private Long memberId;

    @Lob
    @Column(nullable = false)
    private byte[] indexCipher;

    @Column(nullable = false, length = 32)
    private byte[] commitHash;

    @Column(nullable = false)
    private long version;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private VaultIndex(Long memberId, byte[] indexCipher, byte[] commitHash, long version, LocalDateTime updatedAt) {
        this.memberId = memberId;
        this.indexCipher = indexCipher;
        this.commitHash = commitHash;
        this.version = version;
        this.updatedAt = updatedAt;
    }

    public static VaultIndex initialize(Long memberId, byte[] indexCipher, byte[] commitHash, LocalDateTime now) {
        return new VaultIndex(memberId, indexCipher, commitHash, 1L, now);
    }

    public void replace(byte[] newIndexCipher, byte[] newCommitHash, LocalDateTime now) {
        this.indexCipher = newIndexCipher;
        this.commitHash = newCommitHash;
        this.version += 1;
        this.updatedAt = now;
    }
}
