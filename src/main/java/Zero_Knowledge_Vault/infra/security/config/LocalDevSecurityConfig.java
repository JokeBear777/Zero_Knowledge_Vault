package Zero_Knowledge_Vault.infra.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Profile("local")
@Configuration
public class LocalDevSecurityConfig {

    /**
     * Local profile only. Keeps /api/dev/** outside the production API chain so
     * development tokens can be issued without OAuth2/SRP during local Swagger tests.
     */
    @Bean
    @Order(0)
    public SecurityFilterChain localDevApiSecurityChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/dev/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
