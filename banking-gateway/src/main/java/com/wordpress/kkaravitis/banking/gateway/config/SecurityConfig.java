package com.wordpress.kkaravitis.banking.gateway.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${security.expected-audience:banking-gateway}")
    private String expectedAudience;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuer;

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
              .csrf(ServerHttpSecurity.CsrfSpec::disable)
              .authorizeExchange(exchanges -> exchanges
                    .pathMatchers("/actuator/**").permitAll()
                    .pathMatchers("/banking/**").authenticated()
                    .anyExchange().denyAll()
              )
              .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
              .build();
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder() {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withIssuerLocation(issuer).build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> withAudience = jwt -> {
            List<String> aud = jwt.getAudience();
            return (aud != null && aud.contains(expectedAudience))
                  ? OAuth2TokenValidatorResult.success()
                  : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        OAuth2ErrorCodes.INVALID_TOKEN,
                        "Missing required audience: " + expectedAudience,
                        null
                  ));
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
        return decoder;
    }
}
