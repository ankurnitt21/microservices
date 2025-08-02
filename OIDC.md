Of course. Here is a comprehensive `README.md` file that explains the entire OpenID Connect flow and details exactly how your project implements it, using the code you've provided.

---

# OpenID Connect (OIDC) with Google for a Microservices Architecture

This document explains how our application uses OpenID Connect with Google as an Identity Provider to secure a Spring Boot microservices ecosystem.

## Table of Contents
1.  [Understanding the Authentication Flow](#understanding-the-authentication-flow)
2.  [Key Concepts: OAuth 2.0 vs. OpenID Connect](#key-concepts-oauth-20-vs-openid-connect)
3.  [Architectural Roles](#architectural-roles)
4.  [Step-by-Step Implementation in Our Project](#step-by-step-implementation-in-our-project)
    *   [Step 1: Google Cloud Console Setup](#step-1-google-cloud-console-setup)
    *   [Step 2: API Gateway as the OAuth2 Client](#step-2-api-gateway-as-the-oauth2-client)
    *   [Step 3: Backend Services as OAuth2 Resource Servers](#step-3-backend-services-as-oauth2-resource-servers)
    *   [Step 4: Developer Testing Workflow](#step-4-developer-testing-workflow)

---

## Understanding the Authentication Flow

The primary goal is to allow users to log in with their Google account to securely access our backend microservices. The entire process is orchestrated by the API Gateway, ensuring that the backend services remain stateless and only need to validate a token.

The user journey is as follows:

1.  **Request Initiation**: The user tries to access a protected API endpoint on our **API Gateway** (e.g., `http://localhost:8085/api/users`).
2.  **Redirect to Google**: The API Gateway, seeing that the user is not authenticated, redirects them to Google's login page.
3.  **Google Authentication**: The user securely enters their Google credentials and provides consent for our application to access their basic profile information.
4.  **Callback with Code**: Google redirects the user back to the API Gateway with a temporary `authorization_code`.
5.  **Token Exchange (Invisible to User)**: The API Gateway exchanges this `code` directly with Google for an **ID Token (a JWT)** and an Access Token.
6.  **Token Relay**: The API Gateway forwards the original request to the appropriate backend microservice (e.g., `user-backend`). Crucially, it attaches the **ID Token** to this request in the `Authorization: Bearer <token>` header.
7.  **Token Validation**: The backend microservice receives the request, inspects the JWT, and validates its signature, issuer, expiration, and audience.
8.  **Access Granted**: If the token is valid, the microservice processes the request and returns a response. If not, it returns a `401 Unauthorized` error.

## Key Concepts: OAuth 2.0 vs. OpenID Connect

It's important to understand that we are using both protocols.

*   **OAuth 2.0 (The Framework for Authorization)**: Provides the core "Authorization Code Grant" flow (redirecting, exchanging the code for a token). Its primary goal is to grant *delegated access* to resources. The main artifact is the **Access Token**.

*   **OpenID Connect (The Layer for Authentication)**: A thin identity layer built on top of OAuth 2.0. Its primary goal is to prove *who the user is*. The main artifact is the **ID Token**, which is a standardized JSON Web Token (JWT).

We use the OAuth 2.0 flow but specifically request the `openid` scope to ensure we get a JWT (ID Token) back, which is perfect for stateless authentication in a microservices environment.

## Architectural Roles

Our system is divided into two distinct security roles:

1.  **API Gateway (The OAuth2 Client)**: This is the user-facing entry point. It is the only component that directly communicates with Google's login and token endpoints. It is responsible for managing the entire authentication process and creating a session for the user's browser.

2.  **Backend Microservices (The Resource Servers)**: Services like `user-backend`, `product-service`, etc., are protected resources. They are not concerned with *how* the user logs in. Their sole security responsibility is to receive a Bearer Token (JWT), validate it, and if valid, grant access to their specific resources. They are completely stateless.

## Step-by-Step Implementation in Our Project

Here is how the concepts above are translated into the code and configuration of our project.

### Step 1: Google Cloud Console Setup

Before writing any code, we registered our application with Google to establish trust and receive credentials.

1.  **Project Creation**: A project was created in the Google Cloud Console.
2.  **OAuth Consent Screen**: We configured the application name and user support email.
3.  **Credentials**: We created an **OAuth 2.0 Client ID** for a "Web application".
4.  **Redirect URIs**: This is a critical security step. We told Google the only two URLs it is allowed to redirect users back to after a successful login:
    *   `http://localhost:8085/login/oauth2/code/google`: For the main application flow, handled by the API Gateway.
    *   `http://localhost:8085/swagger-ui/oauth2-redirect.html`: For allowing developers to test the API directly from the Swagger UI.
5.  **Client ID and Secret**: Google provided us with a `Client ID` and `Client Secret`, which are stored securely and passed to the API Gateway as environment variables.

### Step 2: API Gateway as the OAuth2 Client

The `api-gateway` service is configured to manage the entire OIDC login flow.

#### a) Configuration (`application.properties`)

This configuration tells Spring Security how to interact with Google as an OpenID Connect provider.

```properties
# Defines "google" as an OIDC provider using its issuer URI for auto-discovery
spring.security.oauth2.client.provider.google.issuer-uri=https://accounts.google.com

# Configures our client application
spring.security.oauth2.client.registration.google.provider=google
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=openid,profile,email
spring.security.oauth2.client.registration.google.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
```
*   The `issuer-uri` is the key that enables full OIDC auto-discovery.
*   The `scope=openid` is what requests the JWT ID Token.

#### b) Security Chain (`SecurityConfig.java`)

This code enables the OAuth2 login process and secures all endpoints.

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(exchange -> exchange
                .anyExchange().authenticated()
            )
            .oauth2Login(withDefaults()); // This single line enables the entire OIDC flow
        return http.build();
    }
}

c) Relaying the Token (IdTokenRelayGatewayFilterFactory.java)

This custom filter is the crucial link between the gateway and the backend services. The default `tokenRelay()` filter sends the Access Token, which is not a JWT. This filter finds the **ID Token (the JWT)** from the authenticated user's session and attaches it to the request being forwarded to the backend.

----------------------------------------------------------------------------
@Component
public class IdTokenRelayGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {
    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> exchange.getPrincipal()
                // ... logic to get OidcUser ...
                .map(oidcUser -> {
                    // Extract the ID Token (the JWT)
                    String idToken = oidcUser.getIdToken().getTokenValue(); 
                    // Add it as a "Bearer" token to the downstream request
                    return withBearerAuth(exchange, idToken); 
                })
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }
    // ... helper methods
}
```

### Step 3: Backend Services as OAuth2 Resource Servers

All backend services (`user-backend`, `product-service`, etc.) share the same simple and robust security configuration.

#### a) Configuration (`application.properties`)

These properties configure the service to validate JWTs issued by Google.

```properties
# Tells the resource server who the trusted token issuer is
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://accounts.google.com

# CRITICAL: Validates that the token was intended for our application
# by checking if the 'aud' claim matches our gateway's client ID.
spring.security.oauth2.resourceserver.jwt.audiences=${GOOGLE_CLIENT_ID}

# Allows for a 60-second difference between system clocks to prevent false expiration errors
spring.security.oauth2.resourceserver.jwt.clock-skew=60s
```The `${GOOGLE_CLIENT_ID}` is passed to each microservice container via the `docker-compose.yml` file.

#### b) Security Chain (`SecurityConfig.java`)

This configuration is much simpler than the gateway's. Its only job is to enable JWT validation.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize
                // Allow public access for Swagger and health checks
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                // Secure everything else
                .anyRequest().authenticated()
            )
            // Configure this service as a Resource Server that validates JWTs
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
```

#### c) API Documentation (`OpenApiConfig.java`)

This class tells the Swagger UI that our API is protected and that it should display the "Authorize" button, allowing developers to provide a Bearer Token for testing.

```java
@Configuration
@OpenAPIDefinition(...) // Provides general API info
@SecurityScheme(
    name = "bearerAuth", // Defines a security scheme named "bearerAuth"
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfig {}
```

### Step 4: Developer Testing Workflow

Since only the gateway can log a user in, developers need a way to get a token for testing backend services directly via Swagger UI.

1.  **Log in via the Gateway**: A developer navigates to the temporary `/token` endpoint on the gateway: `http://localhost:8085/token`.
2.  **Complete Google Login**: This triggers the full OIDC flow.
3.  **Capture Token**: The browser displays a JSON object containing the `id_token`. The developer copies this JWT string.
4.  **Authorize Swagger UI**: The developer navigates to the backend service's Swagger UI (e.g., `http://localhost:8080/swagger-ui.html`), clicks "Authorize", and pastes the captured JWT.
5.  **Test**: All subsequent requests from this Swagger UI will now include the `Authorization` header, allowing the developer to test protected endpoints.