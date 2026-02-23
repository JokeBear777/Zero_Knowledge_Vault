package Zero_Knowledge_Vault.domain.member.service;

import Zero_Knowledge_Vault.domain.auth.entity.MemberAuthPake;
import Zero_Knowledge_Vault.domain.auth.repository.MemberAuthPakeRepository;
import Zero_Knowledge_Vault.domain.member.dto.MeResponseDto;
import Zero_Knowledge_Vault.domain.member.dto.OAuthSignupInfo;
import Zero_Knowledge_Vault.domain.member.dto.SignUpRequestDto;
import Zero_Knowledge_Vault.domain.member.entity.Member;
import Zero_Knowledge_Vault.domain.member.repository.MemberRepository;
import Zero_Knowledge_Vault.domain.member.type.MemberRole;
import Zero_Knowledge_Vault.infra.security.jwt.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.bridge.MessageWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberAuthPakeRepository memberAuthPakeRepository;

    public Optional<Member> findByEmail(String email) {

        return memberRepository.findByEmail(email);
    }

    public Optional<Member> findById(Long id) {
        return memberRepository.findById(id);
    }

    @Transactional
    public void signUpOAuth(OAuthSignupInfo oAuthSignupInfo,
                            SignUpRequestDto dto
    ) {
        if (oAuthSignupInfo.email().isEmpty()) {
            throw new IllegalArgumentException("Email is empty");
        }

        if (dto.getName().isEmpty()) {
            throw new IllegalArgumentException("Name is empty");
        }

        if(dto.getMobile().isEmpty()) {
            throw new IllegalArgumentException("Mobile number is empty");
        }

        if (!isValidEmail(oAuthSignupInfo.email())) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (findByEmail(oAuthSignupInfo.email()).isPresent()) {
            throw new RuntimeException("Email is already in use");
        }
        Member member = Member.builder()
                .email(oAuthSignupInfo.email())
                .name(dto.getName())
                .mobile(dto.getMobile())
                .memberRole(MemberRole.ROLE_USER)
                .build();

        memberRepository.save(member);
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailRegex);
    }

    @Transactional
    public void deActiveAccount(CustomUserPrincipal customUserPrincipal) {
        Member member = memberRepository.findByEmail(customUserPrincipal.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        // 멤버가 클럽에 가입되어잇는지 확인, 추후 추가한다
        //
        //

        member.setMemberRole(MemberRole.ROLE_INACTIVE);
    }

    public MeResponseDto getMeStatus(CustomUserPrincipal user) {

        boolean pakeRegistered = memberAuthPakeRepository.existsById(user.getUserId());

        return MeResponseDto.from(user, pakeRegistered);
    }
}
