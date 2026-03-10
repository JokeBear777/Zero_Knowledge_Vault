package Zero_Knowledge_Vault.domain.vault.service;

import Zero_Knowledge_Vault.domain.member.repository.MemberRepository;
import Zero_Knowledge_Vault.domain.vault.dto.VaultItemDeleteRequest;
import Zero_Knowledge_Vault.domain.vault.dto.VaultItemUpsertRequest;
import Zero_Knowledge_Vault.domain.vault.entity.VaultItem;
import Zero_Knowledge_Vault.domain.vault.repository.VaultIndexQueryRepository;
import Zero_Knowledge_Vault.domain.vault.repository.VaultIndexRepository;
import Zero_Knowledge_Vault.domain.vault.repository.VaultItemQueryRepository;
import Zero_Knowledge_Vault.domain.vault.repository.VaultItemRepository;
import Zero_Knowledge_Vault.global.exception.custom.VaultException;
import Zero_Knowledge_Vault.global.exception.type.VaultErrorCode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class VaultCommandService {

    private final MemberRepository memberRepository;
    private final VaultItemRepository vaultItemRepository;
    private final VaultItemQueryRepository vaultItemQueryRepository;
    private final VaultIndexRepository vaultIndexRepository;
    private final VaultIndexQueryRepository vaultIndexQueryRepository;



    @Transactional
    public void upsertItem(Long memberId, VaultItemUpsertRequest request) {
        LocalDateTime now = LocalDateTime.now();

        byte[] itemKeyCipher = Base64.getDecoder().decode(request.itemCipherBase64());
        byte[] itemCipher = Base64.getDecoder().decode(request.itemCipherBase64());
        byte[] newIndexCipher = Base64.getDecoder().decode(request.newIndexCipherBase64());
        byte[] newCommitHash = Base64.getDecoder().decode(request.newCommitHashBase64());

        vaultItemUpsert(memberId, itemKeyCipher, itemCipher, request, now);
        vaultIndexUpsert(memberId, newIndexCipher, newCommitHash, now, request.expectedIndexVersion());

    }

    @Transactional
    public void deleteItem(Long memberId, VaultItemDeleteRequest request) {
        byte[] newIndexCipher = Base64.getDecoder().decode(request.newIndexCipherBase64());
        byte[] newCommitHash = Base64.getDecoder().decode(request.newCommitHashBase64());

        LocalDateTime now = LocalDateTime.now();

        VaultItem vaultItem = vaultItemRepository.findByMemberIdAndItemId(memberId, request.itemId())
                .orElseThrow(() -> new VaultException(VaultErrorCode.ITEM_NOT_FOUND));

        if (vaultItem.getDeletedAt() != null) {
            throw new VaultException(VaultErrorCode.ITEM_ALREADY_DELETED);
        }

        boolean deleted = vaultItemQueryRepository.tombstoneIfVersionMatches(
                memberId,
                request.itemId(),
                request.expectedItemVersion(),
                now
        );

        if (!deleted) {
            throw new VaultException(VaultErrorCode.ITEM_NOT_FOUND);
        }

        long indexUpdated = vaultIndexQueryRepository.updateIfVersionMatches(
                memberId,
                newIndexCipher,
                newCommitHash,
                request.expectedIndexVersion(),
                now
        );

        if (indexUpdated == 0) {
            throw new VaultException(VaultErrorCode.INDEX_VERSION_CONFLICT);
        }

    }

    private void vaultIndexUpsert(
            Long memberId,
            byte[] newIndexCipher,
            byte[] newCommitHash,
            LocalDateTime now,
            Long expectedIndexVersion
            ) {
        long indexUpdated = vaultIndexQueryRepository.updateIfVersionMatches(
                memberId,
                newIndexCipher,
                newCommitHash,
                expectedIndexVersion,
                now
        );

        //예상 가능한 충돌 결과(서비스에서 해석)는 if로 처리, 진짜 예외적 장애를 트라이캐치 ex)db 로우레벨 장애
        if (indexUpdated == 0) {
            throw new VaultException(VaultErrorCode.INDEX_VERSION_CONFLICT);
        }

    }

    private void vaultItemUpsert(
            Long memberId,
            byte[] itemKeyCipher,
            byte[] itemCipher,
            VaultItemUpsertRequest request,
            LocalDateTime now
    ) {
        Optional<VaultItem> existingOpt = vaultItemRepository.findByMemberIdAndItemId(memberId, request.itemId());

        if (existingOpt.isPresent()) {

            vaultItemQueryRepository.updateIfVersionMatches(
                    memberId,
                    request.itemId(),
                    itemKeyCipher,
                    itemCipher,
                    request.expectedItemVersion(),
                    now
            );
        }
        else {
            long expected = request.expectedItemVersion() == null ? 0L : request.expectedItemVersion();
            if (expected != 0L) {
                throw new VaultException(VaultErrorCode.ITEM_VERSION_CONFLICT);
            }

            VaultItem newItem = VaultItem.createNew(
                    memberId,
                    request.itemId(),
                    itemKeyCipher,
                    itemCipher,
                    now
            );

            vaultItemRepository.save(newItem);
        }

    }

}
