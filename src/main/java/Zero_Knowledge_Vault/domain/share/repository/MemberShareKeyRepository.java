package Zero_Knowledge_Vault.domain.share.repository;

import Zero_Knowledge_Vault.domain.share.entity.MemberShareKey;
import Zero_Knowledge_Vault.domain.share.type.ShareKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberShareKeyRepository extends JpaRepository<MemberShareKey, Long> {

    Optional<MemberShareKey> findTopByMemberMemberIdAndStatusOrderByKeyVersionDesc(
            Long memberId,
            ShareKeyStatus status
    );

    boolean existsByMemberMemberIdAndStatus(Long memberId, ShareKeyStatus status);

    @Query("select coalesce(max(k.keyVersion), 0) from MemberShareKey k where k.member.memberId = :memberId")
    Integer findMaxKeyVersionByMemberId(@Param("memberId") Long memberId);
}
