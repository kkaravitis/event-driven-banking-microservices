package com.wordpress.kkaravitis.banking.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak.exchange")
public record KeycloakTokenExchangeProperties(
      String tokenUri,
      String clientId,
      String clientSecret,
      String audience,
      String scope
) {}
