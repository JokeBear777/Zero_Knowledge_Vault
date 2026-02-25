package Zero_Knowledge_Vault.domain.auth.service;

import Zero_Knowledge_Vault.domain.auth.dto.PakeRegisterRequest;
import Zero_Knowledge_Vault.domain.auth.dto.PakeRegisterResponse;
import Zero_Knowledge_Vault.domain.auth.entity.MemberAuthPake;
import Zero_Knowledge_Vault.domain.auth.policy.AuthPolicy;
import Zero_Knowledge_Vault.domain.auth.repository.MemberAuthPakeRepository;
import Zero_Knowledge_Vault.domain.auth.type.PakeAuthStatus;
import Zero_Knowledge_Vault.domain.member.entity.Member;
import Zero_Knowledge_Vault.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.Base64;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthRegistrationService {

    private final AuthPolicy authPolicy;
    private final MemberAuthPakeRepository memberAuthPakeRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public PakeRegisterResponse register(PakeRegisterRequest request, Long memberId) {
        BigInteger verifier = new BigInteger(request.verifier(), 16);
        byte[] verifierBytes = verifier.toByteArray();
        byte[] saltBytes = Base64.getDecoder().decode(request.saltAuth());
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Cannot find member with id " + memberId));

        PakeAuthStatus status = PakeAuthStatus.ACTIVE;

        MemberAuthPake entity = MemberAuthPake.register(
                member,
                authPolicy.pakeAlgorithm(),
                authPolicy.groupId(),
                saltBytes,
                verifierBytes,
                authPolicy.kdfAlgorithm(),
                authPolicy.kdfParams(),
                status,
                authPolicy.version()
        );

        memberAuthPakeRepository.save(entity);

        return new PakeRegisterResponse(
                status,
                authPolicy.version()
        );
    }



}
