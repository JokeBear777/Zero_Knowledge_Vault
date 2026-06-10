package Zero_Knowledge_Vault.infra.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI zeroKnowledgeVaultOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Zero Knowledge Vault API")
                        .description("OAuth2 login issues PRE_AUTH JWT, SRP step-up issues VAULT_AUTH JWT. Vault and shared item plaintext/key material stays client-side.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                                .description("Paste a PRE_AUTH or VAULT_AUTH access token. Shared item APIs require VAULT_AUTH.")));
    }
}
