package Zero_Knowledge_Vault.infra.security.config;


import Zero_Knowledge_Vault.global.filter.JwtAuthFilter;
import Zero_Knowledge_Vault.global.filter.JwtExceptionFilter;
import Zero_Knowledge_Vault.infra.security.oauth2.handler.OAuth2LoginFailureHandler;
import Zero_Knowledge_Vault.infra.security.oauth2.handler.OAuth2LoginSuccessHandler;
import Zero_Knowledge_Vault.infra.security.oauth2.service.CustomOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {


    private final CustomOAuth2UserService customOauth2UserService;
    private final JwtAuthFilter jwtAuthFilter;
    private final JwtExceptionFilter jwtExceptionFilter;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityChain(HttpSecurity http) throws Exception {

        http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/sign-up", "/api/oauth-info").permitAll()
                        .requestMatchers("/api/vault/unlock/**").hasAuthority("PRE_AUTH")
                        .requestMatchers("/api/vault/**").hasAuthority("VAULT_AUTH")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        //인증 실패 → 401
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"error\":\"Unauthorized\"}");
                        })
                        //권한 부족 → 403
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"error\":\"Forbidden\"}");
                        })
                )
                .addFilterBefore(jwtExceptionFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityChain(HttpSecurity http) throws Exception {

        http
                .securityMatcher("/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/index.html", "/login.html",
                                "/sign-up.html", "/home.html",
                                "/css/**", "/js/**", "/images/**",
                                "/favicon.ico", "/.well-known/**"
                        ).permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .tokenEndpoint(token ->
                                token.accessTokenResponseClient(accessTokenResponseClient())
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler)
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOauth2UserService)
                        )
                );

        return http.build();
    }

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
    accessTokenResponseClient() {

        DefaultAuthorizationCodeTokenResponseClient client =
                new DefaultAuthorizationCodeTokenResponseClient();

        OAuth2AccessTokenResponseHttpMessageConverter tokenConverter =
                new OAuth2AccessTokenResponseHttpMessageConverter();

        tokenConverter.setAccessTokenResponseConverter(params -> {

            System.out.println("NAVER TOKEN RESPONSE = " + params);

            String accessToken = (String) params.get("access_token");

            if (accessToken == null) {
                throw new IllegalArgumentException("access_token missing: " + params);
            }

            return OAuth2AccessTokenResponse.withToken(accessToken)
                    .tokenType(OAuth2AccessToken.TokenType.BEARER)
                    .expiresIn(Long.parseLong(params.get("expires_in").toString()))
                    .refreshToken((String) params.get("refresh_token"))
                    .build();
        });

        RestTemplate restTemplate = new RestTemplate(
                Arrays.asList(
                        new FormHttpMessageConverter(),
                        tokenConverter
                )
        );

        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());

        client.setRestOperations(restTemplate);

        return client;
    }

}

