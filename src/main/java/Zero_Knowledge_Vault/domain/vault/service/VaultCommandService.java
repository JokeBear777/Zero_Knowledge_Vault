package Zero_Knowledge_Vault.domain.vault.service;

import Zero_Knowledge_Vault.domain.member.entity.Member;
import Zero_Knowledge_Vault.domain.member.repository.MemberRepository;
import Zero_Knowledge_Vault.domain.vault.dto.*;
import Zero_Knowledge_Vault.domain.vault.entity.MemberVaultKeyMaterial;
import Zero_Knowledge_Vault.domain.vault.entity.VaultIndex;
import Zero_Knowledge_Vault.domain.vault.entity.VaultItem;
import Zero_Knowledge_Vault.domain.vault.repository.*;
import Zero_Knowledge_Vault.global.exception.custom.VaultException;
import Zero_Knowledge_Vault.global.exception.type.VaultErrorCode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.util.privilegedactions.GetResolvedMemberMethods;
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
    private final MemberVaultKeyMaterialRepository memberVaultKeyMaterialRepository;
    private final VaultKeyService vaultKeyService;

    public SetupInitResponse getSetupInitResponse(Long memberId) {
        Optional<MemberVaultKeyMaterial> vaultKeyMaterial = memberVaultKeyMaterialRepository.findById(memberId);

        if (!vaultKeyMaterial.isPresent()) {
            return vaultKeyService.initialize();
        }
        else {
            throw new VaultException(VaultErrorCode.VAULT_SETUP_ALREADY_COMPLETED);
        }

    }

    @Transactional
    public void setupVault(Long memberId, SetupVaultKeyRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        byte[] saltWrap = Base64.getDecoder().decode(request.saltWrapBase64());
        byte[] wrappedVaultKey = Base64.getDecoder().decode(request.wrappedVaultKeyBase64());
        byte[] indexCipher = Base64.getDecoder().decode(request.indexCipherBase64());
        byte[] commitHash = Base64.getDecoder().decode(request.commitHashBase64());

        Optional<MemberVaultKeyMaterial> vaultKeyMaterial = memberVaultKeyMaterialRepository.findById(memberId);

        if (vaultKeyMaterial.isPresent()) {
            throw new VaultException(VaultErrorCode.VAULT_SETUP_ALREADY_COMPLETED);
        }

        MemberVaultKeyMaterial material = MemberVaultKeyMaterial.register(
                member,
                wrappedVaultKey,
                saltWrap,
                request.wrapKdfAlgorithm(),
                request.wrapKdfParams()
        );
        memberVaultKeyMaterialRepository.save(material);

        VaultIndex vaultIndex = VaultIndex.initialize(
                memberId,
                indexCipher,
                commitHash,
                LocalDateTime.now()
        );
        vaultIndexRepository.save(vaultIndex);
    }


    @Transactional
    public void setupVaultIndex(Long memberId, SetupVaultIndexRequest request) {
        if (vaultIndexRepository.existsById(memberId)) {
            throw new VaultException(VaultErrorCode.VAULT_ALREADY_INITIALIZED);
        }

        LocalDateTime now = LocalDateTime.now();

        byte[] indexCipher = Base64.getDecoder().decode(request.indexCipherBase64());
        byte[] commitHash = Base64.getDecoder().decode(request.commitHashBase64());

        VaultIndex vaultIndex = VaultIndex.initialize(
                memberId,
                indexCipher,
                commitHash,
                now
        );

        vaultIndexRepository.save(vaultIndex);
    }

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
