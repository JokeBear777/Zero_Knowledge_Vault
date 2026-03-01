package Zero_Knowledge_Vault.domain.auth.service;

import Zero_Knowledge_Vault.domain.auth.dto.PakeAuthInitRequest;
import Zero_Knowledge_Vault.domain.auth.dto.SrpAuthInitResponse;
import Zero_Knowledge_Vault.domain.auth.dto.SrpChallenge;
import Zero_Knowledge_Vault.domain.auth.entity.MemberAuthPake;
import Zero_Knowledge_Vault.domain.auth.policy.AuthPolicy;
import Zero_Knowledge_Vault.domain.auth.policy.SrpPolicy;
import Zero_Knowledge_Vault.domain.auth.policy.SrpSessionPolicy;
import Zero_Knowledge_Vault.domain.auth.repository.MemberAuthPakeRepository;
import Zero_Knowledge_Vault.domain.auth.srp.SrpGroup;
import Zero_Knowledge_Vault.domain.auth.srp.SrpService;
import Zero_Knowledge_Vault.domain.auth.srp.SrpSession;
import Zero_Knowledge_Vault.domain.auth.srp.SrpSessionStore;
import Zero_Knowledge_Vault.infra.security.jwt.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SrpAuthenticationService {

    private final SrpSessionStore srpSessionStore;
    private final MemberAuthPakeRepository memberAuthPakeRepository;
    private final SrpService srpService;
    private final SrpSessionPolicy srpSessionPolicy;
    private final AuthPolicy authPolicy;
    private final SrpPolicy srpPolicy;


    public SrpAuthInitResponse initPake(PakeAuthInitRequest request, CustomUserPrincipal customUserPrincipal) {

        MemberAuthPake memberAuthPake = memberAuthPakeRepository.findByMemberId(customUserPrincipal.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        BigInteger verifier = new BigInteger(1, memberAuthPake.getVerifier());
        byte[] saltBytes = memberAuthPake.getSaltAuth();

        String saltAuthBase64 =
                Base64.getEncoder().encodeToString(saltBytes);

        SrpChallenge srpChallenge =  srpService.generateChallenge(
                request.A(),
                verifier,
                SrpGroup.N,
                SrpGroup.g
        );

        SrpSession srpSession = srpSessionStore.create(
                customUserPrincipal.getUserId(),
                request.A(),
                srpChallenge.B(),
                srpChallenge.b(),
                srpSessionPolicy.ttlSeconds()
        );


        return new SrpAuthInitResponse(
                srpSession.id(),
                saltAuthBase64,
                srpSession.BHex(),
                authPolicy.groupId(),
                srpPolicy.hashAlgorithm(),
                authPolicy.kdfAlgorithm().toString(),
                authPolicy.kdfParams()
        );
    }

}
