package Zero_Knowledge_Vault.domain.shareditem.repository;

import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemMember;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemMemberStatus;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
