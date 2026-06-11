package Zero_Knowledge_Vault.domain.shareditem.repository;

import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemMember;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemMemberStatus;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SharedItemMemberRepository extends JpaRepository<SharedItemMember, Long> {

    Optional<SharedItemMember> findBySharedItemSharedItemIdAndMemberMemberId(
            Long sharedItemId,
            Long memberId
    );

    Optional<SharedItemMember> findBySharedItemSharedItemIdAndMemberMemberIdAndStatus(
            Long sharedItemId,
            Long memberId,
            SharedItemMemberStatus status
    );

    boolean existsBySharedItemSharedItemIdAndMemberMemberId(
            Long sharedItemId,
            Long memberId
    );

    @Query("""
            select m
              from SharedItemMember m
              join fetch m.sharedItem i
             where m.member.memberId = :memberId
               and m.status = :memberStatus
               and i.status = :itemStatus
             order by i.createdAt desc
            """)
    List<SharedItemMember> findAccessibleItems(
            @Param("memberId") Long memberId,
            @Param("memberStatus") SharedItemMemberStatus memberStatus,
            @Param("itemStatus") SharedItemStatus itemStatus
    );

    List<SharedItemMember> findBySharedItemSharedItemIdAndStatusOrderByCreatedAtAsc(
            Long sharedItemId,
            SharedItemMemberStatus status
    );

    List<SharedItemMember> findBySharedItemSharedItemIdOrderByCreatedAtAsc(Long sharedItemId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SharedItemMember m
               set m.encryptedItemKeyBase64 = :encryptedItemKeyBase64,
                   m.recipientKeyVersion = :recipientKeyVersion,
                   m.updatedAt = :updatedAt
             where m.sharedItem.sharedItemId = :sharedItemId
               and m.member.memberId = :memberId
               and m.status = :status
            """)
    int updateEncryptedItemKey(
            @Param("sharedItemId") Long sharedItemId,
            @Param("memberId") Long memberId,
            @Param("encryptedItemKeyBase64") String encryptedItemKeyBase64,
            @Param("recipientKeyVersion") Integer recipientKeyVersion,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("status") SharedItemMemberStatus status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SharedItemMember m
               set m.status = :revokedStatus,
                   m.encryptedItemKeyBase64 = null,
                   m.revokedAt = :revokedAt,
                   m.updatedAt = :revokedAt
             where m.sharedItem.sharedItemId = :sharedItemId
               and m.member.memberId in :memberIds
               and m.status = :activeStatus
            """)
    int revokeMembers(
            @Param("sharedItemId") Long sharedItemId,
            @Param("memberIds") List<Long> memberIds,
            @Param("activeStatus") SharedItemMemberStatus activeStatus,
            @Param("revokedStatus") SharedItemMemberStatus revokedStatus,
            @Param("revokedAt") LocalDateTime revokedAt
    );
}
