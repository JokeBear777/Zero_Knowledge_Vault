package Zero_Knowledge_Vault.domain.vault.share.service;

import Zero_Knowledge_Vault.domain.member.entity.Member;
import Zero_Knowledge_Vault.domain.member.repository.MemberRepository;
import Zero_Knowledge_Vault.domain.vault.share.dto.request.CreateShareKeyRequest;
import Zero_Knowledge_Vault.domain.vault.share.dto.request.RegenerateShareKeyRequest;
import Zero_Knowledge_Vault.domain.vault.share.dto.response.DeleteShareKeyResponse;
import Zero_Knowledge_Vault.domain.vault.share.dto.response.MyShareKeyResponse;
import Zero_Knowledge_Vault.domain.vault.share.dto.response.PublicShareKeyResponse;
import Zero_Knowledge_Vault.domain.vault.share.entity.MemberShareKey;
import Zero_Knowledge_Vault.domain.vault.share.exception.ShareKeyException;
import Zero_Knowledge_Vault.domain.vault.share.repository.MemberShareKeyRepository;
import Zero_Knowledge_Vault.domain.vault.share.type.ShareKeyStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberShareKeyService {

    private static final String SUPPORTED_ALGORITHM = "RSA-OAEP-256";
    private static final int MAX_KEY_TEXT_LENGTH = 65535;

    private final MemberRepository memberRepository;
    private final MemberShareKeyRepository memberShareKeyRepository;

    public MyShareKeyResponse getMyShareKey(Long memberId) {
        return memberShareKeyRepository
                .findTopByMemberMemberIdAndStatusOrderByKeyVersionDesc(memberId, ShareKeyStatus.ACTIVE)
                .map(MyShareKeyResponse::from)
                .orElseGet(MyShareKeyResponse::empty);
    }

    @Transactional
    public MyShareKeyResponse createShareKey(Long memberId, CreateShareKeyRequest request) {
        validateRequest(request.publicKeyBase64(), request.encryptedPrivateKeyBase64(), request.algorithm());

        if (memberShareKeyRepository.existsByMemberMemberIdAndStatus(memberId, ShareKeyStatus.ACTIVE)) {
            throw new ShareKeyException(
                    HttpStatus.CONFLICT,
                    "SHARE_KEY_ALREADY_EXISTS",
                    "Active share key already exists"
            );
        }

        Member member = findMember(memberId);
        MemberShareKey key = MemberShareKey.create(
                member,
                1,
                request.publicKeyBase64(),
                request.encryptedPrivateKeyBase64(),
                request.algorithm()
        );

        try {
            return MyShareKeyResponse.from(memberShareKeyRepository.saveAndFlush(key));
        } catch (DataIntegrityViolationException e) {
            throw new ShareKeyException(
                    HttpStatus.CONFLICT,
                    "SHARE_KEY_VERSION_CONFLICT",
                    "Share key version already exists"
            );
        }
    }

    public PublicShareKeyResponse getPublicShareKey(Long targetMemberId) {
        return memberShareKeyRepository
                .findTopByMemberMemberIdAndStatusOrderByKeyVersionDesc(targetMemberId, ShareKeyStatus.ACTIVE)
                .map(PublicShareKeyResponse::from)
                .orElseThrow(() -> new ShareKeyException(
                        HttpStatus.NOT_FOUND,
                        "PUBLIC_SHARE_KEY_NOT_FOUND",
                        "Active public share key not found"
                ));
    }

    @Transactional
    public DeleteShareKeyResponse deleteMyShareKey(Long memberId) {
        MemberShareKey key = memberShareKeyRepository
                .findTopByMemberMemberIdAndStatusOrderByKeyVersionDesc(memberId, ShareKeyStatus.ACTIVE)
                .orElseThrow(() -> new ShareKeyException(
                        HttpStatus.NOT_FOUND,
                        "SHARE_KEY_NOT_FOUND",
                        "Active share key not found"
                ));

        key.delete();
        return new DeleteShareKeyResponse(true);
    }

    @Transactional
    public MyShareKeyResponse regenerateShareKey(Long memberId, RegenerateShareKeyRequest request) {
        validateRequest(request.publicKeyBase64(), request.encryptedPrivateKeyBase64(), request.algorithm());

        memberShareKeyRepository
                .findTopByMemberMemberIdAndStatusOrderByKeyVersionDesc(memberId, ShareKeyStatus.ACTIVE)
                .ifPresent(key -> key.rotate(LocalDateTime.now()));

        Member member = findMember(memberId);
        Integer nextVersion = memberShareKeyRepository.findMaxKeyVersionByMemberId(memberId) + 1;
        MemberShareKey newKey = MemberShareKey.create(
                member,
                nextVersion,
                request.publicKeyBase64(),
                request.encryptedPrivateKeyBase64(),
                request.algorithm()
        );

        try {
            return MyShareKeyResponse.from(memberShareKeyRepository.saveAndFlush(newKey));
        } catch (DataIntegrityViolationException e) {
            throw new ShareKeyException(
                    HttpStatus.CONFLICT,
                    "SHARE_KEY_VERSION_CONFLICT",
                    "Share key version already exists"
            );
        }
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ShareKeyException(
                        HttpStatus.NOT_FOUND,
                        "MEMBER_NOT_FOUND",
                        "Member not found"
                ));
    }

    private void validateRequest(String publicKeyBase64, String encryptedPrivateKeyBase64, String algorithm) {
        validateAlgorithm(algorithm);
        validateBase64("publicKeyBase64", publicKeyBase64);
        validateBase64("encryptedPrivateKeyBase64", encryptedPrivateKeyBase64);
    }

    private void validateAlgorithm(String algorithm) {
        if (!SUPPORTED_ALGORITHM.equals(algorithm)) {
            throw new ShareKeyException(
                    HttpStatus.BAD_REQUEST,
                    "UNSUPPORTED_SHARE_KEY_ALGORITHM",
                    "Unsupported share key algorithm"
            );
        }
    }

    private void validateBase64(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new ShareKeyException(
                    HttpStatus.BAD_REQUEST,
                    "SHARE_KEY_FIELD_REQUIRED",
                    fieldName + " is required"
            );
        }

        if (value.length() > MAX_KEY_TEXT_LENGTH) {
            throw new ShareKeyException(
                    HttpStatus.BAD_REQUEST,
                    "SHARE_KEY_FIELD_TOO_LONG",
                    fieldName + " is too long"
            );
        }

        try {
            Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new ShareKeyException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_SHARE_KEY_BASE64",
                    fieldName + " must be valid Base64"
            );
        }
    }
}
