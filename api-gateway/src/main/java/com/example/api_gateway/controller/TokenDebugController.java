package com.example.api_gateway.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * THIS IS A TEMPORARY CONTROLLER FOR DEBUGGING PURPOSES ONLY.
 * It exposes the user's JWT ID token. This should be disabled or
 * removed in a production environment.
 */
@RestController
public class TokenDebugController {

    @GetMapping("/token")
    public Mono<Map<String, String>> getToken(@AuthenticationPrincipal Mono<OidcUser> oidcUserMono) {
        return oidcUserMono
                .map(oidcUser -> {
                    String idToken = oidcUser.getIdToken().getTokenValue();
                    return Map.of("id_token", idToken);
                })
                .switchIfEmpty(Mono.just(Map.of("error", "User not authenticated or not an OIDC user")));
    }
}