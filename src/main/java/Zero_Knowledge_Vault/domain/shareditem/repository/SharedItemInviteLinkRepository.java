package Zero_Knowledge_Vault.domain.shareditem.repository;

import Zero_Knowledge_Vault.domain.shareditem.entity.SharedItemInviteLink;
import Zero_Knowledge_Vault.domain.shareditem.type.SharedItemInviteLinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SharedItemInviteLinkRepository extends JpaRepository<SharedItemInviteLink, Long> {

    Optional<SharedItemInviteLink> findByInviteTokenHashAndStatusAndExpiresAtAfter(
            String inviteTokenHash,
            SharedItemInviteLinkStatus status,
            LocalDateTime now
    );
}
