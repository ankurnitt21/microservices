package com.example.order_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Order Service API",
                version = "1.0",
                description = "This API exposes endpoints to manage users."
        ),
        // This applies the security scheme to ALL endpoints
        security = {
                @SecurityRequirement(name = "bearerAuth")
        }
)
@SecurityScheme(
        name = "bearerAuth", // This is the name that will be referenced by @SecurityRequirement
        type = SecuritySchemeType.HTTP, // We are using HTTP-based security
        scheme = "bearer", // The scheme is "bearer" for JWTs
        bearerFormat = "JWT" // This tells Swagger UI the token is a JWT
)
public class OpenApiConfig {
}