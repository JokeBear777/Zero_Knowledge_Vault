package Zero_Knowledge_Vault.domain.dev.service;

import Zero_Knowledge_Vault.domain.dev.dto.request.DevLoginRequest;
import Zero_Knowledge_Vault.domain.dev.dto.response.DevAuthResponse;
import Zero_Knowledge_Vault.domain.dev.dto.response.DevMemberResponse;
import Zero_Knowledge_Vault.domain.member.entity.Member;
import Zero_Knowledge_Vault.domain.member.repository.MemberRepository;
import Zero_Knowledge_Vault.domain.member.type.MemberRole;
import Zero_Knowledge_Vault.infra.security.AuthLevel;
import Zero_Knowledge_Vault.infra.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Profile("local")
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DevAuthService {

    private static final String DEV_MOBILE = "010-0000-0000";

    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public DevAuthResponse login(DevLoginRequest request, AuthLevel authLevel) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseGet(() -> memberRepository.save(Member.builder()
                        .email(request.email())
                        .name(request.nickname())
                        .mobile(DEV_MOBILE)
                        .memberRole(MemberRole.ROLE_USER)
                        .build()));

        return issueToken(member, authLevel);
    }

    public List<DevMemberResponse> getMembers() {
        return memberRepository.findAll().stream()
                .map(DevMemberResponse::from)
                .toList();
    }

    public DevAuthResponse issueToken(Long memberId, AuthLevel authLevel) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        return issueToken(member, authLevel);
    }

    private DevAuthResponse issueToken(Member member, AuthLevel authLevel) {
        String accessToken = jwtUtil.generateAccessToken(
                member.getMemberId(),
                member.getEmail(),
                member.getMemberRole().toString(),
                authLevel.name()
        );

        return DevAuthResponse.of(member, authLevel, accessToken);
    }
}
