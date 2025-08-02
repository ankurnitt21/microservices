package com.example.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private final IdTokenRelayGatewayFilterFactory idTokenRelayFilter;

    public GatewayConfig(IdTokenRelayGatewayFilterFactory idTokenRelayFilter) {
        this.idTokenRelayFilter = idTokenRelayFilter;
    }


    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service", r -> r.path("/api/users/**")
                        .filters(f -> f.filter(idTokenRelayFilter.apply(new Object())))
                        .uri("lb://user-backend"))
                .route("product-service", r -> r.path("/api/products/**")
                        .filters(f -> f.filter(idTokenRelayFilter.apply(new Object())))
                        .uri("lb://product-service"))
                .route("inventory-service", r -> r.path("/api/inventory/**")
                        .filters(f -> f.filter(idTokenRelayFilter.apply(new Object())))
                        .uri("lb://inventory-service"))
                .route("order-service", r -> r.path("/api/orders/**")
                        .filters(f -> f.filter(idTokenRelayFilter.apply(new Object())))
                        .uri("lb://order-service"))
                .build();
    }
}