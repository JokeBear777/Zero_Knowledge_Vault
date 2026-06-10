package Zero_Knowledge_Vault.domain.dev.controller;

import Zero_Knowledge_Vault.domain.dev.dto.request.DevLoginRequest;
import Zero_Knowledge_Vault.domain.dev.dto.response.DevAuthResponse;
import Zero_Knowledge_Vault.domain.dev.dto.response.DevMemberResponse;
import Zero_Knowledge_Vault.domain.dev.service.DevAuthService;
import Zero_Knowledge_Vault.infra.security.AuthLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Profile("local")
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
@Tag(
        name = "Dev Auth",
        description = "Local profile only APIs for creating test members and issuing JWTs. Never active outside the local profile."
)
public class DevAuthController {

    private final DevAuthService devAuthService;

    @PostMapping("/login")
    @Operation(
            summary = "Create or reuse a local test member and issue a JWT",
            description = """
                    LOCAL PROFILE ONLY. Creates or reuses a test member by email and returns a JWT.
                    Defaults to VAULT_AUTH so Swagger can test /api/shared-items/** without Naver OAuth2 or SRP.
                    This endpoint is not registered outside the local profile.
                    """
    )
    public ResponseEntity<DevAuthResponse> login(
            @Valid @RequestBody DevLoginRequest request,
            @Parameter(description = "Token auth level to issue", example = "VAULT_AUTH")
            @RequestParam(defaultValue = "VAULT_AUTH") AuthLevel authLevel
    ) {
        return ResponseEntity.ok(devAuthService.login(request, authLevel));
    }

    @GetMapping("/members")
    @Operation(
            summary = "List local test members",
            description = "LOCAL PROFILE ONLY. Lists members available for local multi-user shared vault testing."
    )
    public ResponseEntity<List<DevMemberResponse>> getMembers() {
        return ResponseEntity.ok(devAuthService.getMembers());
    }

    @PostMapping("/token/{memberId}")
    @Operation(
            summary = "Issue a JWT for an existing local test member",
            description = """
                    LOCAL PROFILE ONLY. Issues a token for switching Swagger Authorize between owner/memberA/memberB.
                    Defaults to VAULT_AUTH because shared item APIs require VAULT_AUTH.
                    """
    )
    public ResponseEntity<DevAuthResponse> issueToken(
            @Parameter(description = "Existing member id", example = "1")
            @PathVariable Long memberId,
            @Parameter(description = "Token auth level to issue", example = "VAULT_AUTH")
            @RequestParam(defaultValue = "VAULT_AUTH") AuthLevel authLevel
    ) {
        return ResponseEntity.ok(devAuthService.issueToken(memberId, authLevel));
    }
}
