package Zero_Knowledge_Vault.domain.shareditem.repository;

import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItem;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface SharedItemRepository extends JpaRepository<SharedItem, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SharedItem i
               set i.titleCipherBase64 = :titleCipherBase64,
                   i.itemCipherBase64 = :itemCipherBase64,
                   i.version = i.version + 1,
                   i.updatedAt = :updatedAt
             where i.sharedItemId = :sharedItemId
               and i.version = :expectedVersion
               and i.status = :status
            """)
    int updateCipherIfVersionMatches(
            @Param("sharedItemId") Long sharedItemId,
            @Param("expectedVersion") Long expectedVersion,
            @Param("titleCipherBase64") String titleCipherBase64,
            @Param("itemCipherBase64") String itemCipherBase64,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("status") SharedItemStatus status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SharedItem i
               set i.titleCipherBase64 = :titleCipherBase64,
                   i.itemCipherBase64 = :itemCipherBase64,
                   i.version = i.version + 1,
                   i.keyVersion = i.keyVersion + 1,
                   i.membershipVersion = i.membershipVersion + 1,
                   i.updatedAt = :updatedAt
             where i.sharedItemId = :sharedItemId
               and i.version = :expectedVersion
               and i.keyVersion = :expectedKeyVersion
               and i.membershipVersion = :expectedMembershipVersion
               and i.status = :status
            """)
    int rotateCipherIfVersionsMatch(
            @Param("sharedItemId") Long sharedItemId,
            @Param("expectedVersion") Long expectedVersion,
            @Param("expectedKeyVersion") Long expectedKeyVersion,
            @Param("expectedMembershipVersion") Long expectedMembershipVersion,
            @Param("titleCipherBase64") String titleCipherBase64,
            @Param("itemCipherBase64") String itemCipherBase64,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("status") SharedItemStatus status
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            update SharedItem i
               set i.membershipVersion = i.membershipVersion + 1,
                   i.updatedAt = :updatedAt
             where i.sharedItemId = :sharedItemId
               and i.status = :status
            """)
    int incrementMembershipVersion(
            @Param("sharedItemId") Long sharedItemId,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("status") SharedItemStatus status
    );
}
