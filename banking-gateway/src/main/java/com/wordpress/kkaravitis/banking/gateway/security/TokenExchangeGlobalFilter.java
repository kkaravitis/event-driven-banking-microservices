package com.wordpress.kkaravitis.banking.gateway.security;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TokenExchangeGlobalFilter implements GlobalFilter, Ordered {

    private final KeycloakTokenExchangeClient exchangeClient;

    public TokenExchangeGlobalFilter(KeycloakTokenExchangeClient exchangeClient) {
        this.exchangeClient = exchangeClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();
        if (!path.startsWith("/banking/transfer/")) {
            return chain.filter(exchange);
        }

        return exchange.getPrincipal()
              .cast(Authentication.class)
              .flatMap(auth -> {
                  if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
                      return chain.filter(exchange);
                  }

                  String userToken = jwtAuth.getToken().getTokenValue();

                  return exchangeClient.exchange(userToken)
                        .flatMap(internalToken -> {
                            var mutatedRequest = exchange.getRequest().mutate()
                                  .headers(h -> {
                                      h.remove(HttpHeaders.AUTHORIZATION);
                                      h.add(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken);
                                  })
                                  .build();

                            return chain.filter(exchange.mutate().request(mutatedRequest).build());
                        });
              });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
