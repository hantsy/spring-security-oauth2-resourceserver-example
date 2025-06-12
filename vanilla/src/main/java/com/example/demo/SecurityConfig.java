package com.example.demo;

import com.example.demo.AudienceValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private static final String AUDIENCE = "http://demo-service";

    @Bean
    public SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(org.springframework.http.HttpMethod.GET, "/greeting/**").hasRole("USER")
                        .anyExchange().permitAll()
                )
                .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder(OAuth2ResourceServerProperties properties) {
        // val jwkSetUri = properties.jwt.jwkSetUri
        // val jwtDecoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build()

        String issuerUri = properties.getJwt().getIssuerUri();
        NimbusReactiveJwtDecoder jwtDecoder = ReactiveJwtDecoders.fromOidcIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(AUDIENCE);
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

        jwtDecoder.setJwtValidator(withAudience);
        return jwtDecoder;
    }

    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(customRolesConverter());
        return converter;
    }

    private Converter<Jwt, Flux<GrantedAuthority>> customRolesConverter() {
        return jwt -> {
            Object realmAccessObj = jwt.getClaims().get("realm_access");
            Collection<String> kcRoles = null;
            if (realmAccessObj instanceof Map<?, ?> realmAccess) {
                Object rolesObj = realmAccess.get("roles");
                if (rolesObj instanceof Collection<?>) {
                    kcRoles = (Collection<String>) rolesObj;
                }
            }
            Object otherRolesObj = jwt.getClaims().get("roles");
            Collection<String> otherRoles = null;
            if (otherRolesObj instanceof Collection<?>) {
                otherRoles = (Collection<String>) otherRolesObj;
            }
            Collection<String> roles = kcRoles != null ? kcRoles : (otherRoles != null ? otherRoles : List.of());

            return Flux.fromIterable(roles)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role));
        };
    }
}
