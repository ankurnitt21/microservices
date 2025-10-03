# 🔐 Google Login Setup Guide (OIDC/OAuth2)

## 🤔 What is OIDC and Why Do We Need It?

**Simple Explanation:**
Instead of creating our own username/password system, we let users login with their existing Google account. It's like using "Login with Google" button you see on many websites.

**Benefits:**
- ✅ Users don't need to remember another password
- ✅ Google handles all the security (2FA, password recovery, etc.)
- ✅ We don't store sensitive password data
- ✅ Users trust Google more than unknown websites

## 🏗️ How It Works (Simple Version)

```
1. User clicks "Login" → 2. Redirected to Google → 3. User enters Google password
                                    ↓
6. User can access our app ← 5. We get user info ← 4. Google sends us a "token"
```

**Detailed Flow:**
1. User tries to access our API (e.g., get list of users)
2. Our API Gateway says "You need to login first"
3. User gets redirected to Google's login page
4. User enters their Google username/password
5. Google asks "Do you want to allow this app to access your basic info?"
6. User clicks "Yes"
7. Google sends user back to our app with a special "token"
8. Our app uses this token to verify the user is real
9. User can now access our APIs

## 🛠️ Step-by-Step Setup

### Step 1: Create Google OAuth Application

**Why?** Google needs to know about our application before it can trust it.

1. **Go to Google Cloud Console**
   - Open https://console.cloud.google.com/
   - Login with your Google account

2. **Create or Select Project**
   - If you don't have a project: Click "Select a project" → "New Project"
   - Give it a name like "My Microservices App"
   - Click "Create"

3. **Enable Required APIs**
   - Go to "APIs & Services" → "Library"
   - Search for "Google+ API" 
   - Click it and press "Enable"

4. **Configure OAuth Consent Screen**
   - Go to "APIs & Services" → "OAuth consent screen"
   - Choose "External" (unless you have Google Workspace)
   - Fill in required fields:
     - App name: "My Microservices App"
     - User support email: your email
     - Developer email: your email
   - Click "Save and Continue" through all steps

5. **Create OAuth Credentials**
   - Go to "APIs & Services" → "Credentials"
   - Click "Create Credentials" → "OAuth 2.0 Client IDs"
   - Choose "Web application"
   - Name: "Microservices API Gateway"
   - **Authorized redirect URIs** (VERY IMPORTANT):
     - `http://localhost:8085/login/oauth2/code/google`
     - `http://localhost:8085/swagger-ui/oauth2-redirect.html`
   - Click "Create"

6. **Save Your Credentials**
   - Copy the "Client ID" (looks like: 123456789-abcdef.apps.googleusercontent.com)
   - Copy the "Client Secret" (looks like: GOCSPX-abcdef123456)
   - **Keep these safe!** We'll use them in Step 2

### Step 2: Configure Your Application

**Create Environment File:**

In your project root, create/edit `.env` file:

```env
# Google OAuth Credentials (replace with your actual values)
GOOGLE_CLIENT_ID=123456789-abcdef.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-abcdef123456

# Database settings
DB_USER=postgres
DB_PASSWORD=yourpassword123
DB_URL=jdbc:postgresql://postgres-db:5432/postgres
```

**Important:** 
- Replace the example values with your real Google credentials
- Never commit this file to Git (it should be in .gitignore)

### Step 3: Understanding the Code (No Changes Needed)

**API Gateway Configuration (api-gateway/src/main/resources/application.properties):**

```properties
# This tells Spring Boot how to talk to Google
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=openid,profile,email
spring.security.oauth2.client.provider.google.issuer-uri=https://accounts.google.com
```

**What each line means:**
- `client-id` & `client-secret`: Your Google credentials
- `scope=openid,profile,email`: What info we want from Google (name, email)
- `issuer-uri`: Google's server address for authentication

**Security Configuration (api-gateway/.../SecurityConfig.java):**

```java
.oauth2Login(withDefaults()); // This enables Google login
```

**Business Services Configuration:**

Each service (user-backend, product-service, etc.) has:

```properties
# These services don't handle login, they just check if the token is valid
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://accounts.google.com
spring.security.oauth2.resourceserver.jwt.audiences=${GOOGLE_CLIENT_ID}
```

**What this means:**
- Services receive a "JWT token" from the API Gateway
- They ask Google "Is this token real and valid?"
- If yes, they process the request
- If no, they return "401 Unauthorized"

## 🧪 Testing the Authentication

### Method 1: Browser Test (Easiest)

1. **Start the application:**
   ```bash
   docker-compose up -d --build
   ```

2. **Try to access a protected endpoint:**
   - Open browser to: http://localhost:8085/api/users
   - You should be redirected to Google login

3. **Login with Google:**
   - Enter your Google email/password
   - Click "Allow" when Google asks for permissions
   - You should see `[]` (empty user list) - this means it worked!

### Method 2: Get a Token for API Testing

1. **Get authentication token:**
   - Go to: http://localhost:8085/token
   - Login with Google
   - You'll see JSON with an `id_token` field
   - Copy the long token string (starts with `eyJ`)

2. **Use token in API calls:**
   ```bash
   # Replace YOUR_TOKEN_HERE with the actual token
   curl -H "Authorization: Bearer YOUR_TOKEN_HERE" \
        http://localhost:8080/api/users
   ```

3. **Use token in Swagger UI:**
   - Go to http://localhost:8080/swagger-ui.html
   - Click "Authorize" button
   - Paste your token in the "Value" field
   - Click "Authorize"
   - Now you can test all API endpoints

## 🔍 What Happens Behind the Scenes

### When You Access API Gateway:

```java
// 1. API Gateway checks: "Is user logged in?"
.authorizeExchange(exchange -> exchange.anyExchange().authenticated())

// 2. If not logged in: "Redirect to Google"
.oauth2Login(withDefaults())
```

### When Google Sends You Back:

```java
// 3. API Gateway receives the token and creates a session
// 4. When forwarding requests to services, it adds the token:
String idToken = oidcUser.getIdToken().getTokenValue();
return withBearerAuth(exchange, idToken);
```

### When Business Services Receive Requests:

```java
// 5. Each service validates the token:
.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
```

## 🚨 Common Issues and Solutions

### Issue 1: "redirect_uri_mismatch" Error

**Problem:** Google shows this error when you try to login.

**Solution:** 
- Check your Google Cloud Console redirect URIs exactly match:
  - `http://localhost:8085/login/oauth2/code/google`
  - `http://localhost:8085/swagger-ui/oauth2-redirect.html`
- Make sure you're accessing via `localhost`, not `127.0.0.1`

### Issue 2: "invalid_client" Error

**Problem:** Wrong Client ID or Client Secret.

**Solution:**
- Double-check your `.env` file has correct values
- Make sure there are no extra spaces or quotes
- Verify the credentials in Google Cloud Console

### Issue 3: "401 Unauthorized" on Business Services

**Problem:** Token validation is failing.

**Solution:**
- Make sure all services have the same `GOOGLE_CLIENT_ID` in docker-compose.yml
- Check service logs: `docker-compose logs user-backend`
- Verify your token isn't expired (tokens expire after 1 hour)

### Issue 4: Services Can't Reach Google

**Problem:** JWT validation fails with connection errors.

**Solution:**
- Check internet connection
- Verify Docker containers can reach external URLs
- Try restarting: `docker-compose restart`

## 🔧 Advanced Configuration

### Custom Token Endpoint (For Development)

The API Gateway has a special endpoint to get tokens for testing:

**File:** `api-gateway/.../TokenDebugController.java`

```java
@GetMapping("/token")
public Mono<Map<String, Object>> getToken(Authentication authentication) {
    // Returns the JWT token for manual testing
}
```

**Usage:**
1. Go to http://localhost:8085/token
2. Login with Google
3. Copy the `id_token` value
4. Use it in your API testing tools

### Understanding JWT Tokens

The token you get is a JWT (JSON Web Token) that contains:

```json
{
  "iss": "https://accounts.google.com",
  "aud": "your-client-id",
  "sub": "user-google-id",
  "email": "user@gmail.com",
  "name": "User Name",
  "exp": 1234567890
}
```

**Fields explained:**
- `iss`: Who created the token (Google)
- `aud`: Who the token is for (your app)
- `sub`: Unique user ID
- `email`: User's email address
- `name`: User's display name
- `exp`: When token expires

## 🎯 Security Best Practices

### ✅ What We Do Right:

1. **Token Validation:** Every service validates tokens independently
2. **HTTPS in Production:** Use HTTPS for redirect URIs in production
3. **Short Token Expiry:** Tokens expire after 1 hour
4. **Audience Validation:** We check tokens are meant for our app
5. **Secure Storage:** Client secrets are in environment variables

### ⚠️ Important Notes:

1. **Never commit `.env` file** - Contains sensitive credentials
2. **Use HTTPS in production** - HTTP is only OK for local development
3. **Rotate secrets regularly** - Change Google credentials periodically
4. **Monitor token usage** - Watch for suspicious authentication patterns

## 🚀 Production Deployment

When deploying to production:

1. **Update Redirect URIs in Google Console:**
   - Add your production domain: `https://yourdomain.com/login/oauth2/code/google`
   - Add Swagger URI: `https://yourdomain.com/swagger-ui/oauth2-redirect.html`

2. **Use Environment Variables:**
   ```bash
   export GOOGLE_CLIENT_ID="your-production-client-id"
   export GOOGLE_CLIENT_SECRET="your-production-client-secret"
   ```

3. **Enable HTTPS:**
   - Use a reverse proxy (nginx, Traefik)
   - Get SSL certificates (Let's Encrypt)

## 📚 Additional Resources

- [Google OAuth 2.0 Documentation](https://developers.google.com/identity/protocols/oauth2)
- [Spring Security OAuth 2.0 Guide](https://spring.io/guides/tutorials/spring-boot-oauth2/)
- [JWT Token Debugger](https://jwt.io/) - Decode and inspect JWT tokens
- [OAuth 2.0 Simplified](https://aaronparecki.com/oauth-2-simplified/) - Great explanation

---

**🎉 Congratulations!** You now understand how Google authentication works in your microservices system. Users can securely login with their Google accounts, and all your services can verify their identity!
