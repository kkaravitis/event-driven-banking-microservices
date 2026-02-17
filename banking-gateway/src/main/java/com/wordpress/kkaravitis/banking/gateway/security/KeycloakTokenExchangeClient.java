package com.wordpress.kkaravitis.banking.gateway.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordpress.kkaravitis.banking.gateway.config.KeycloakTokenExchangeProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class KeycloakTokenExchangeClient {

    private final WebClient webClient;
    private final KeycloakTokenExchangeProperties keycloakTokenExchangeProperties;

    public KeycloakTokenExchangeClient(WebClient.Builder builder, KeycloakTokenExchangeProperties props) {
        this.webClient = builder.build();
        this.keycloakTokenExchangeProperties = props;
    }

    public Mono<String> exchange(String userAccessToken) {
        return webClient.post()
              .uri(keycloakTokenExchangeProperties.tokenUri())
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(BodyInserters
                    .fromFormData("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
                    .with("client_id", keycloakTokenExchangeProperties.clientId())
                    .with("client_secret", keycloakTokenExchangeProperties.clientSecret())
                    .with("subject_token", userAccessToken)
                    .with("subject_token_type", "urn:ietf:params:oauth:token-type:access_token")
                    .with("requested_token_type", "urn:ietf:params:oauth:token-type:access_token")
                    .with("scope", keycloakTokenExchangeProperties.scope())
                    .with("audience", keycloakTokenExchangeProperties.audience())
              )
              .retrieve()
              .bodyToMono(TokenResponse.class)
              .map(TokenResponse::accessToken);
    }

    record TokenResponse(@JsonProperty("access_token") String accessToken) {}
}
