package Zero_Knowledge_Vault.domain.vault.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "vault_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_vault_item_user_item",
                columnNames = {"user_id", "item_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VaultItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 36)
    private String itemId;

    @Lob
    @Column(nullable = false)
    private byte[] itemKeyCipher;

    @Lob
    @Column(nullable = false)
    private byte[] itemCipher;

    @Column(nullable = false)
    private long version;

    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private VaultItem(Long memberId, String itemId, byte[] itemKeyCipher, byte[] itemCipher,
                      long version, LocalDateTime deletedAt, LocalDateTime updatedAt) {
        this.memberId = memberId;
        this.itemId = itemId;
        this.itemKeyCipher = itemKeyCipher;
        this.itemCipher = itemCipher;
        this.version = version;
        this.deletedAt = deletedAt;
        this.updatedAt = updatedAt;
    }

    public static VaultItem createNew(Long memberId, String itemId, byte[] itemKeyCipher, byte[] itemCipher,
                                      LocalDateTime now) {
        return new VaultItem(
                memberId,
                itemId,
                itemKeyCipher,
                itemCipher,
                1L,
                null,
                now
        );
    }

    public void markDeleted(LocalDateTime now) {
        this.deletedAt = now;
        this.version += 1;
        this.updatedAt = now;
    }

    public void restore(byte[] itemKeyCipher, byte[] itemCipher, LocalDateTime now) {
        this.itemKeyCipher = itemKeyCipher;
        this.itemCipher = itemCipher;
        this.deletedAt = null;
        this.version += 1;
        this.updatedAt = now;
    }
}