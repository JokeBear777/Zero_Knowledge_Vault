package Zero_Knowledge_Vault.domain.auth.service;

import Zero_Knowledge_Vault.domain.member.entity.Member;
import Zero_Knowledge_Vault.infra.security.AuthLevel;
import Zero_Knowledge_Vault.infra.security.jwt.GeneratedToken;
import Zero_Knowledge_Vault.infra.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final JwtUtil jwtUtil;

    public GeneratedToken issuePreAuth(Member member) {
        return jwtUtil.generateToken(
                member.getMemberId(),
                member.getEmail(),
                member.getMemberRole().toString(),
                AuthLevel.PRE_AUTH.name()
        );
    }

    public GeneratedToken issueStepUp(Member member) {
        return jwtUtil.generateToken(
                member.getMemberId(),
                member.getEmail(),
                member.getMemberRole().toString(),
                AuthLevel.VAULT_AUTH.name()
        );
    }

}
