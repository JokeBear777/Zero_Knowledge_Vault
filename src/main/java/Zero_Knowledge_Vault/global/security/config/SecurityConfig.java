package Zero_Knowledge_Vault.global.security.config;


import Zero_Knowledge_Vault.global.filter.JwtAuthFilter;
import Zero_Knowledge_Vault.global.filter.JwtExceptionFilter;
import Zero_Knowledge_Vault.global.security.oauth2.handler.OAuth2LoginFailureHandler;
import Zero_Knowledge_Vault.global.security.oauth2.handler.OAuth2LoginSuccessHandler;
import Zero_Knowledge_Vault.global.security.oauth2.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final JwtExceptionFilter jwtExceptionFilter;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/index.html", "/login.html",
                                "/sign-up.html", "/home.html",
                                "/css/**", "/js/**", "/images/**",
                                "/favicon.ico", "/.well-known/**"
                        ).permitAll()

                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                        .requestMatchers("/api/sign-up", "/api/oauth-info").permitAll()

                        // Unlock 단계
                        .requestMatchers("/api/vault/unlock/**")
                        .hasAuthority("PRE_AUTH")

                        // Vault 실제 접근
                        .requestMatchers("/api/vault/**")
                        .hasAuthority("VAULT_AUTH")

                        .anyRequest().authenticated()
                )

                .oauth2Login(outh -> outh
                        .tokenEndpoint(token ->
                                token.accessTokenResponseClient(accessTokenResponseClient())
                        )
                        //.loginPage("/oauth2-login")
                        .failureHandler(oAuth2LoginFailureHandler) // OAuth2 로그인 실패시 처리할 핸들러를 지정해준다.
                        .successHandler(oAuth2LoginSuccessHandler) // OAuth2 로그인 성공시 처리할 핸들러를 지정해준다.
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOauth2UserService) // 사용자 정보 서비스 설정
                        )
                );

        return http
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtExceptionFilter, JwtAuthFilter.class)
                .build();
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

