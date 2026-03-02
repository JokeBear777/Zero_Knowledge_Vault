package Zero_Knowledge_Vault.infra.auth.srp;

import Zero_Knowledge_Vault.domain.auth.srp.SrpSession;
import Zero_Knowledge_Vault.domain.auth.srp.SrpSessionStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SrpSessionStoreInMemoryImpl implements SrpSessionStore {

    private final Map<String, SrpSession> map = new ConcurrentHashMap<>();

    @Override
    public SrpSession create(Long userId, String AHex, String BHex, String bHex, Long ttlSeconds) {
        String id = UUID.randomUUID().toString();
        SrpSession s = new SrpSession(id, userId, AHex, BHex, bHex, Instant.now().plusSeconds(ttlSeconds), false);
        map.put(id, s);
        return s;
    }

    @Override
    public Optional<SrpSession> findValid(String id) {
        SrpSession s = map.get(id);
        if (s == null) return Optional.empty();
        if (s.used()) return Optional.empty();
        if (Instant.now().isAfter(s.expiresAt())) {
            map.remove(id);
            return Optional.empty();
        }
        return Optional.of(s);
    }

    @Override
    public void markUsed(String id) {
        map.computeIfPresent(id, (k, v) ->
                new SrpSession(v.id(), v.userId(), v.AHex(), v.BHex(), v.bHex(), v.expiresAt(), true)
        );
    }

    @Override
    public void delete(String id) {
        map.remove(id);
    }


    @Scheduled(fixedDelay = 60000)
    public void cleanupExpired() {
        map.values().removeIf(SrpSession::isExpired);
    }
}
