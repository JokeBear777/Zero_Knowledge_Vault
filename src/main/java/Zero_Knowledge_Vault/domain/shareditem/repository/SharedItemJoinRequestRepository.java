package Zero_Knowledge_Vault.domain.shareditem.repository;

import Zero_Knowledge_Vault.domain.member.entity.Member;
import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemJoinRequest;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemJoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SharedItemJoinRequestRepository extends JpaRepository<SharedItemJoinRequest, Long> {

    Optional<SharedItemJoinRequest> findBySharedItemSharedItemIdAndRequesterMemberId(
            Long sharedItemId,
            Long requesterMemberId
    );

    List<SharedItemJoinRequest> findBySharedItemSharedItemIdAndStatusOrderByRequestedAtAsc(
            Long sharedItemId,
            SharedItemJoinRequestStatus status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SharedItemJoinRequest r
               set r.status = :nextStatus,
                   r.decidedAt = :decidedAt,
                   r.decidedBy = :decidedBy
             where r.joinRequestId = :joinRequestId
               and r.sharedItem.sharedItemId = :sharedItemId
               and r.status = :expectedStatus
            """)
    int decideIfPending(
            @Param("joinRequestId") Long joinRequestId,
            @Param("sharedItemId") Long sharedItemId,
            @Param("expectedStatus") SharedItemJoinRequestStatus expectedStatus,
            @Param("nextStatus") SharedItemJoinRequestStatus nextStatus,
            @Param("decidedAt") LocalDateTime decidedAt,
            @Param("decidedBy") Member decidedBy
    );
}
