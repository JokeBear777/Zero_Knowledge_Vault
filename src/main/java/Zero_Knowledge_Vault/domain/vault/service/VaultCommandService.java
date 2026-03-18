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
import org.springframework.dao.DataIntegrityViolationException;
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
        Optional<MemberVaultKeyMaterial> vaultKeyMaterial =
                memberVaultKeyMaterialRepository.findById(memberId);

        if (vaultKeyMaterial.isPresent()) {
            throw new VaultException(VaultErrorCode.VAULT_SETUP_ALREADY_COMPLETED);
        }

        return vaultKeyService.initialize();
    }

    @Transactional
    public void setupVault(Long memberId, SetupVaultKeyRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new VaultException(VaultErrorCode.MEMBER_NOT_FOUND));

        byte[] saltWrap = Base64.getDecoder().decode(request.saltWrapBase64());
        byte[] wrappedVaultKey = Base64.getDecoder().decode(request.wrappedVaultKeyBase64());
        byte[] indexCipher = Base64.getDecoder().decode(request.indexCipherBase64());
        byte[] commitHash = Base64.getDecoder().decode(request.commitHashBase64());

        if (memberVaultKeyMaterialRepository.findById(memberId).isPresent()) {
            throw new VaultException(VaultErrorCode.VAULT_SETUP_ALREADY_COMPLETED);
        }

        try {
            MemberVaultKeyMaterial material = MemberVaultKeyMaterial.register(
                    member,
                    wrappedVaultKey,
                    saltWrap,
                    request.wrapKdfAlgorithm(),
                    request.wrapKdfParams()
            );
            memberVaultKeyMaterialRepository.saveAndFlush(material);

            VaultIndex vaultIndex = VaultIndex.initialize(
                    memberId,
                    indexCipher,
                    commitHash,
                    LocalDateTime.now()
            );
            vaultIndexRepository.saveAndFlush(vaultIndex);

        } catch (DataIntegrityViolationException e) {
            // PK/UNIQUE 제약 중복 등 setup 경쟁 상황
            throw new VaultException(VaultErrorCode.VAULT_SETUP_ALREADY_COMPLETED);
        }
    }

    @Transactional
    public void setupVaultIndex(Long memberId, SetupVaultIndexRequest request) {
        LocalDateTime now = LocalDateTime.now();

        byte[] indexCipher = Base64.getDecoder().decode(request.indexCipherBase64());
        byte[] commitHash = Base64.getDecoder().decode(request.commitHashBase64());

        try {
            if (vaultIndexRepository.existsById(memberId)) {
                throw new VaultException(VaultErrorCode.VAULT_ALREADY_INITIALIZED);
            }

            VaultIndex vaultIndex = VaultIndex.initialize(
                    memberId,
                    indexCipher,
                    commitHash,
                    now
            );

            vaultIndexRepository.saveAndFlush(vaultIndex);
        } catch (DataIntegrityViolationException e) {
            throw new VaultException(VaultErrorCode.VAULT_ALREADY_INITIALIZED);
        }
    }

    @Transactional
    public void upsertItem(Long memberId, VaultItemUpsertRequest request) {
        LocalDateTime now = LocalDateTime.now();

        byte[] itemKeyCipher = Base64.getDecoder().decode(request.itemKeyCipherBase64());
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
            throw new VaultException(VaultErrorCode.ITEM_VERSION_CONFLICT);
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
        Optional<VaultItem> existingOpt =
                vaultItemRepository.findByMemberIdAndItemId(memberId, request.itemId());

        if (existingOpt.isPresent()) {
            VaultItem existing = existingOpt.get();

            // soft delete 정책 명확화
            // 정책 1: tombstone 상태면 복구 금지
            if (existing.getDeletedAt() != null) {
                throw new VaultException(VaultErrorCode.ITEM_ALREADY_DELETED);
            }

            boolean updated = vaultItemQueryRepository.updateIfVersionMatches(
                    memberId,
                    request.itemId(),
                    itemKeyCipher,
                    itemCipher,
                    request.expectedItemVersion(),
                    now
            );

            if (!updated) {
                throw new VaultException(VaultErrorCode.ITEM_VERSION_CONFLICT);
            }

            return;
        }

        long expected = request.expectedItemVersion() == null ? 0L : request.expectedItemVersion();
        if (expected != 0L) {
            throw new VaultException(VaultErrorCode.ITEM_VERSION_CONFLICT);
        }

        try {
            VaultItem newItem = VaultItem.createNew(
                    memberId,
                    request.itemId(),
                    itemKeyCipher,
                    itemCipher,
                    now
            );

            vaultItemRepository.saveAndFlush(newItem);
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 (member_id, item_id) insert 시도한 경우
            throw new VaultException(VaultErrorCode.ITEM_VERSION_CONFLICT);
        }
    }
}