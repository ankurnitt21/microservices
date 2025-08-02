package com.example.api_gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class IdTokenRelayGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> exchange.getPrincipal()
                .filter(principal -> principal instanceof OAuth2AuthenticationToken)
                .cast(OAuth2AuthenticationToken.class)
                .flatMap(this::getOidcUser)
                .map(oidcUser -> {
                    String idToken = oidcUser.getIdToken().getTokenValue();
                    return withBearerAuth(exchange, idToken);
                })
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    private Mono<OidcUser> getOidcUser(OAuth2AuthenticationToken authentication) {
        if (authentication.getPrincipal() instanceof OidcUser) {
            return Mono.just((OidcUser) authentication.getPrincipal());
        }
        return Mono.empty();
    }

    private ServerWebExchange withBearerAuth(ServerWebExchange exchange, String token) {
        return exchange.mutate()
                .request(r -> r.headers(headers -> headers.setBearerAuth(token)))
                .build();
    }
}