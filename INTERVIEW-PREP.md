# Spring Boot Interview Prep – Complete Web Layer Guide

Comprehensive, in-depth notes on Spring Boot web layer annotations and types for senior-level interviews.

---

## @Validated

### What
Triggers Bean Validation (JSR-303/380) for method parameters and `@RequestBody` DTOs, enabling declarative validation.

### Why
- **Early Validation**: Catch invalid data before business logic
- **Consistent Error Handling**: Standardized validation responses
- **Security**: Prevent malformed data from reaching services
- **API Documentation**: Self-documenting validation rules

### Internal Working
1. **MethodValidationPostProcessor** intercepts method calls
2. **ValidationInterceptor** applies constraint validators
3. **ConstraintViolationException** thrown on violations
4. **MethodArgumentNotValidException** for web requests
5. **Exception handlers** convert to HTTP responses

### Deep Dive
```java
// DTO with comprehensive validation
public class UserRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be 2-50 characters")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 320, message = "Email too long")
    private String email;
    
    @NotNull(message = "Age is required")
    @Min(value = 18, message = "Must be at least 18")
    @Max(value = 120, message = "Invalid age")
    private Integer age;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
    private String phone;
}

// Controller with validation
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Validated @RequestBody UserRequest request) {
        // Validation happens before this method executes
        User user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userMapper.toResponse(user));
    }
    
    // Validation groups for different scenarios
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Validated({UpdateGroup.class}) @RequestBody UserRequest request) {
        // Only validates fields marked with UpdateGroup
        return ResponseEntity.ok(userService.updateUser(id, request));
    }
}

// Validation groups
public interface CreateGroup {}
public interface UpdateGroup {}
```

### Advanced Features
- **Validation Groups**: Different validation rules for different operations
- **Custom Validators**: Create domain-specific validation logic
- **Cross-field Validation**: Validate relationships between fields
- **Conditional Validation**: Apply rules based on other field values

### Exception Handling
```java
@ControllerAdvice
public class ValidationExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Validation Failed");
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage()));
        
        problemDetail.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problemDetail);
    }
}
```

### Sequence Flow
```
Client -> Controller: POST /api/users (JSON)
Controller -> Bean Validation: Validate @RequestBody and method params
Bean Validation -> Controller: OK or violations
alt violations
  Bean Validation -> Exception: ConstraintViolationException / MethodArgumentNotValidException
  Exception -> @ControllerAdvice: map to ProblemDetail 400
  @ControllerAdvice -> Client: 400 Validation Failed (errors)
else valid
  Controller -> Service: createUser(request)
  Service -> Repository: persist user
  Repository -> Service: saved User
  Service -> Controller: User
  Controller -> Client: 201 Created (UserResponse)
end
```

---

## @RequestBody

### What
Binds HTTP request body (JSON/XML) to Java method parameters using HttpMessageConverters.

### Why
- **Type Safety**: Convert JSON to strongly-typed objects
- **Automatic Deserialization**: No manual JSON parsing
- **Content Negotiation**: Support multiple formats (JSON, XML)
- **Validation Integration**: Works seamlessly with `@Validated`

### Internal Working
1. **DispatcherServlet** receives HTTP request
2. **HandlerMethodArgumentResolver** identifies `@RequestBody` parameter
3. **HttpMessageConverter** (Jackson) deserializes JSON to Java object
4. **Content-Type** header determines converter selection
5. **ObjectMapper** performs actual conversion

### Deep Dive
```java
// Multiple content types support
@PostMapping(value = "/users", 
             consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest request) {
    return ResponseEntity.ok(userService.createUser(request));
}

// Custom deserialization with Jackson
@JsonDeserialize(using = CustomUserDeserializer.class)
public class UserRequest {
    private String name;
    private String email;
    // Custom deserialization logic
}

// Request body with headers
@PostMapping("/users")
public ResponseEntity<UserResponse> createUser(
        @RequestBody UserRequest request,
        @RequestHeader("X-Client-Version") String clientVersion,
        @RequestHeader("X-Request-ID") String requestId) {
    
    // Access both body and headers
    return userService.createUser(request, clientVersion, requestId);
}
```

### Advanced Features
- **Custom Message Converters**: Support new formats
- **Content Negotiation**: Multiple response formats
- **Streaming**: Handle large request bodies
- **Validation**: Automatic constraint validation

### Error Handling
```java
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<ProblemDetail> handleMalformedJson(
        HttpMessageNotReadableException ex) {
    
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problemDetail.setTitle("Invalid JSON");
    problemDetail.setDetail("Request body contains malformed JSON");
    
    return ResponseEntity.badRequest().body(problemDetail);
}
```

### Sequence Flow
```
Client -> Controller: POST /users (Content-Type: application/json)
Controller -> HandlerMethodArgumentResolver: detect @RequestBody
HandlerMethodArgumentResolver -> HttpMessageConverter: deserialize JSON
HttpMessageConverter -> ObjectMapper: map to UserRequest
ObjectMapper -> Controller: UserRequest instance
alt malformed JSON
  ObjectMapper -> Exception: HttpMessageNotReadableException
  Exception -> @ControllerAdvice: 400 Invalid JSON
  @ControllerAdvice -> Client: 400 ProblemDetail
else ok
  Controller -> Service: createUser(request)
  Service -> Controller: User
  Controller -> Client: 200/201 UserResponse
end
```

---

## ResponseEntity<T>

### What
Complete HTTP response builder providing control over status codes, headers, and body content.

### Why
- **Precise Control**: Exact status codes and headers
- **RESTful Design**: Proper HTTP semantics
- **API Documentation**: Self-documenting responses
- **Client Integration**: Predictable response format

### Internal Working
1. **ResponseEntity** wraps response data
2. **HttpServletResponse** receives status and headers
3. **HttpMessageConverter** serializes body to JSON/XML
4. **Content-Type** determined by Accept header or default

### Deep Dive
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // Resource creation with Location header
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Validated @RequestBody UserRequest request) {
        UserResponse user = userService.createUser(request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .location(URI.create("/api/users/" + user.getId()))
                .header("X-Resource-ID", user.getId().toString())
                .body(user);
    }
    
    // Conditional responses
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(userMapper.toResponse(user)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Custom headers and caching
    @GetMapping("/{id}/avatar")
    public ResponseEntity<byte[]> getUserAvatar(@PathVariable Long id) {
        return userService.getUserAvatar(id)
                .map(avatar -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(30)))
                        .eTag("\"" + avatar.hashCode() + "\"")
                        .body(avatar))
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Error responses with custom headers
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("User Not Found");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("path", "/api/users/" + ex.getUserId());
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .header("X-Error-Code", "USER_NOT_FOUND")
                .body(problemDetail);
    }
}
```

### Sequence Flow
```
Controller -> ResponseEntity: build status, headers, body
ResponseEntity -> HttpServletResponse: set status and headers
ResponseEntity -> HttpMessageConverter: serialize body
HttpMessageConverter -> Client: response bytes (Content-Type negotiated)
```

### Status Code Patterns
- **200 OK**: Successful GET, PUT, PATCH
- **201 Created**: Successful POST with resource creation
- **204 No Content**: Successful DELETE, PUT without body
- **400 Bad Request**: Validation errors, malformed requests
- **401 Unauthorized**: Authentication required
- **403 Forbidden**: Insufficient permissions
- **404 Not Found**: Resource doesn't exist
- **409 Conflict**: Business rule violations
- **422 Unprocessable Entity**: Semantic errors
- **500 Internal Server Error**: Unexpected server errors

---

## @PathVariable

### What
Binds URI template variables to method parameters, extracting path segments as typed values.

### Why
- **RESTful URLs**: Clean, hierarchical resource identification
- **Type Safety**: Automatic type conversion
- **URL Validation**: Built-in path validation
- **SEO Friendly**: Human-readable URLs

### Internal Working
1. **UriTemplate** parses URL patterns
2. **PathVariableMethodArgumentResolver** extracts variables
3. **Type conversion** using PropertyEditors or Converters
4. **Validation** if constraints are present

### Deep Dive
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // Simple path variable
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Multiple path variables
    @GetMapping("/{userId}/orders/{orderId}")
    public ResponseEntity<OrderResponse> getUserOrder(
            @PathVariable Long userId,
            @PathVariable Long orderId) {
        return userService.findUserOrder(userId, orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Path variables with validation
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable @Min(1) Long id) {
        // Validation happens before method execution
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Custom path variable names
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @PathVariable("userId") Long id) {
        return userService.findProfileById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Path variables with regex patterns
    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Path variables with custom types
    @GetMapping("/{uuid}")
    public ResponseEntity<UserResponse> getUserByUuid(
            @PathVariable UUID uuid) {
        return userService.findByUuid(uuid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
```

### Sequence Flow
```
Client -> DispatcherServlet: GET /api/users/42
DispatcherServlet -> PathVariableResolver: extract id=42
PathVariableResolver -> Controller: invoke method(Long id)
Controller -> Service: findById(42)
Service -> Repository: SELECT ... WHERE id=42
Repository -> Service: Optional<User>
Service -> Controller: Optional<User>
alt found
  Controller -> Client: 200 UserResponse
else not found
  Controller -> Client: 404 Not Found
end
```

### Advanced Features
- **Regex Patterns**: Validate path variable format
- **Custom Converters**: Convert to domain objects
- **Optional Path Variables**: Handle optional path segments
- **Matrix Variables**: Support for matrix parameters

### Error Handling
```java
@ExceptionHandler(MethodArgumentTypeMismatchException.class)
public ResponseEntity<ProblemDetail> handleTypeMismatch(
        MethodArgumentTypeMismatchException ex) {
    
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problemDetail.setTitle("Invalid Path Variable");
    problemDetail.setDetail("Path variable '" + ex.getName() + 
                           "' must be of type " + ex.getRequiredType().getSimpleName());
    
    return ResponseEntity.badRequest().body(problemDetail);
}
```

---

## @RequestParam

### What
Binds HTTP request parameters (query string, form data) to method parameters.

### Why
- **Query Parameters**: Support for filtering, pagination, sorting
- **Form Data**: Handle HTML form submissions
- **Optional Parameters**: Flexible API design
- **Type Conversion**: Automatic string-to-type conversion

### Internal Working
1. **Request parameter extraction** from query string or form data
2. **Type conversion** using PropertyEditors or Converters
3. **Default value handling** for optional parameters
4. **Validation** if constraints are present

### Deep Dive
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // Basic query parameters
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort) {
        
        Pageable pageable = PageRequest.of(page, size, 
            Sort.by(Sort.Direction.fromString(sort.split(",")[1]), 
                   sort.split(",")[0]));
        
        Page<User> users = userService.findAll(pageable);
        return ResponseEntity.ok(users.map(userMapper::toResponse));
    }
    
    // Optional parameters with validation
    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) @Min(18) Integer minAge,
            @RequestParam(required = false) @Max(100) Integer maxAge) {
        
        List<User> users = userService.search(name, email, minAge, maxAge);
        return ResponseEntity.ok(users.stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList()));
    }
    
    // Multiple values (arrays/lists)
    @GetMapping("/bulk")
    public ResponseEntity<List<UserResponse>> getUsersByIds(
            @RequestParam List<Long> ids) {
        
        List<User> users = userService.findByIds(ids);
        return ResponseEntity.ok(users.stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList()));
    }
    
    // Custom parameter names
    @GetMapping("/filter")
    public ResponseEntity<List<UserResponse>> filterUsers(
            @RequestParam("user_name") String name,
            @RequestParam("user_email") String email) {
        
        List<User> users = userService.findByNameAndEmail(name, email);
        return ResponseEntity.ok(users.stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList()));
    }
    
    // Complex objects as parameters
    @GetMapping("/advanced-search")
    public ResponseEntity<List<UserResponse>> advancedSearch(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate createdAfter,
            @RequestParam(required = false) String[] roles) {
        
        SearchCriteria criteria = SearchCriteria.builder()
                .name(name)
                .email(email)
                .createdAfter(createdAfter)
                .roles(Arrays.asList(roles))
                .build();
        
        List<User> users = userService.advancedSearch(criteria);
        return ResponseEntity.ok(users.stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList()));
    }
}
```

### Sequence Flow
```
Client -> Controller: GET /api/users?page=1&size=10&sort=name,desc
Controller -> Type Conversion: convert params to int/String
Controller -> Pageable: PageRequest.of(1,10,Sort.by(DESC,"name"))
Controller -> Service: findAll(pageable)
Service -> Repository: findAll(pageable)
Repository -> DB: SELECT ... ORDER BY name DESC LIMIT 10 OFFSET 10
DB -> Repository: rows + total count
Repository -> Service: Page<User>
Service -> Controller: Page<User>
Controller -> Client: 200 Page<UserResponse> (JSON)
```

### Advanced Features
- **Default Values**: Handle missing parameters gracefully
- **Required Parameters**: Enforce mandatory query parameters
- **Multiple Values**: Support for arrays and collections
- **Custom Names**: Map different parameter names
- **Type Conversion**: Automatic string-to-object conversion

---

## @RestController

### What
Meta-annotation combining `@Controller` and `@ResponseBody`, designed for RESTful web services.

### Why
- **REST APIs**: Optimized for JSON/XML responses
- **Simplified Development**: No need for view resolution
- **Content Negotiation**: Automatic format selection
- **HTTP Semantics**: Proper RESTful behavior

### Internal Working
1. **@Controller**: Marks class as web controller
2. **@ResponseBody**: Serializes return values to HTTP response body
3. **HttpMessageConverter**: Handles JSON/XML serialization
4. **Content Negotiation**: Determines response format

### Deep Dive
```java
@RestController
@RequestMapping("/api/users")
@Validated
@Slf4j
public class UserController {
    
    private final UserService userService;
    private final UserMapper userMapper;
    
    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }
    
    // CRUD operations
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {
        
        log.info("Fetching users - page: {}, size: {}, sort: {}", page, size, sort);
        
        Pageable pageable = createPageable(page, size, sort);
        Page<User> users = userService.findAll(pageable);
        
        return ResponseEntity.ok(users.map(userMapper::toResponse));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        log.info("Fetching user with ID: {}", id);
        
        return userService.findById(id)
                .map(user -> {
                    log.debug("User found: {}", user.getName());
                    return ResponseEntity.ok(userMapper.toResponse(user));
                })
                .orElseGet(() -> {
                    log.warn("User not found with ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }
    
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Validated @RequestBody UserRequest request) {
        
        log.info("Creating user: {}", request.getName());
        
        User user = userService.createUser(request);
        UserResponse response = userMapper.toResponse(user);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .location(URI.create("/api/users/" + user.getId()))
                .body(response);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Validated @RequestBody UserRequest request) {
        
        log.info("Updating user with ID: {}", id);
        
        return userService.updateUser(id, request)
                .map(user -> {
                    log.debug("User updated successfully: {}", user.getName());
                    return ResponseEntity.ok(userMapper.toResponse(user));
                })
                .orElseGet(() -> {
                    log.warn("User not found for update with ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Deleting user with ID: {}", id);
        
        boolean deleted = userService.deleteUser(id);
        if (deleted) {
            log.debug("User deleted successfully with ID: {}", id);
            return ResponseEntity.noContent().build();
        } else {
            log.warn("User not found for deletion with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
    
    // Utility method
    private Pageable createPageable(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && 
            sortParts[1].equalsIgnoreCase("desc") ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        String sortBy = sortParts[0];
        
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}
```

### Sequence Flow
```
Client -> DispatcherServlet: HTTP request
DispatcherServlet -> Controller: invoke handler method
Controller -> Service: perform business logic
Service -> Controller: domain model / DTO
Controller (@ResponseBody) -> HttpMessageConverter: serialize return value
HttpMessageConverter -> Client: JSON/XML response
```

### Best Practices
- **Single Responsibility**: One controller per resource
- **Consistent Naming**: Follow RESTful conventions
- **Error Handling**: Use `@ControllerAdvice` for global exception handling
- **Logging**: Add appropriate logging for debugging
- **Validation**: Use `@Validated` for input validation
- **Documentation**: Use OpenAPI/Swagger annotations

---

## @RequestMapping

### What
Meta-annotation for mapping web requests to handler methods, supporting various HTTP methods and URL patterns.

### Why
- **URL Mapping**: Define request-to-method mappings
- **HTTP Method Support**: RESTful method handling
- **Content Negotiation**: Multiple content types
- **Request Conditions**: Headers, parameters, media types

### Internal Working
1. **HandlerMapping** registers URL patterns
2. **RequestMatcher** matches incoming requests
3. **HandlerMethod** invokes appropriate method
4. **Content Negotiation** determines response format

### Deep Dive
```java
@RestController
@RequestMapping(
    value = "/api/users",
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
    consumes = MediaType.APPLICATION_JSON_VALUE
)
public class UserController {
    
    // Method-level mappings
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        // GET /api/users/{id}
    }
    
    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest request) {
        // POST /api/users
    }
    
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id, 
            @RequestBody UserRequest request) {
        // PUT /api/users/{id}
    }
    
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        // DELETE /api/users/{id}
    }
    
    // Advanced request mapping
    @RequestMapping(
        value = "/search",
        method = {RequestMethod.GET, RequestMethod.POST},
        params = {"name", "email"},
        headers = {"X-API-Version=1.0"},
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<UserResponse>> searchUsers(
            @RequestParam String name,
            @RequestParam String email,
            HttpServletRequest request) {
        
        // Only matches requests with name and email params
        // and X-API-Version=1.0 header
    }
    
    // Path patterns with wildcards
    @RequestMapping(value = "/**/admin", method = RequestMethod.GET)
    public ResponseEntity<List<UserResponse>> getAdminUsers() {
        // Matches /api/users/admin, /api/users/any/path/admin
    }
    
    // Ant-style patterns
    @RequestMapping(value = "/{id}/orders/*", method = RequestMethod.GET)
    public ResponseEntity<List<OrderResponse>> getUserOrders(@PathVariable Long id) {
        // Matches /api/users/1/orders/anything
    }
}
```

### Sequence Flow
```
Client -> HandlerMapping: match URL and HTTP method
HandlerMapping -> HandlerAdapter: select handler method
HandlerAdapter -> Controller: invoke mapped method
Controller -> ResponseEntity/body: return value
ResponseEntity/body -> HttpMessageConverter: serialize if needed
HttpMessageConverter -> Client: response
```

### HTTP Method Annotations
```java
// Equivalent to @RequestMapping(method = RequestMethod.GET)
@GetMapping("/{id}")
public ResponseEntity<UserResponse> getUser(@PathVariable Long id) { }

// Equivalent to @RequestMapping(method = RequestMethod.POST)
@PostMapping
public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest request) { }

// Equivalent to @RequestMapping(method = RequestMethod.PUT)
@PutMapping("/{id}")
public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UserRequest request) { }

// Equivalent to @RequestMapping(method = RequestMethod.DELETE)
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) { }

// Equivalent to @RequestMapping(method = RequestMethod.PATCH)
@PatchMapping("/{id}")
public ResponseEntity<UserResponse> patchUser(@PathVariable Long id, @RequestBody Map<String, Object> updates) { }
```

### Advanced Features
- **Path Variables**: `{id}`, `{name}`, `{id:[0-9]+}`
- **Wildcards**: `*`, `**`, `?`
- **Ant Patterns**: `*.json`, `**/admin`
- **Request Conditions**: headers, params, consumes, produces
- **Content Negotiation**: Multiple media types

---

## @GetMapping

### What
Shortcut for `@RequestMapping(method = RequestMethod.GET)` to retrieve resources.

### Why
- **Read Operations**: Fetch resources without side effects
- **Caching**: Safe and idempotent, friendly to caches and CDNs
- **Semantics**: Aligns with REST conventions

### Internal Working
1. **HandlerMapping** resolves GET handler
2. **Argument Resolvers** bind `@PathVariable`, `@RequestParam`, `@RequestHeader`
3. **Return Handling** via `@ResponseBody`/`HttpMessageConverter`
4. **Conditional GET** via ETag/Last-Modified if configured

### Deep Dive
```java
@GetMapping
public ResponseEntity<Page<UserResponse>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "id,asc") String sort) {
    Pageable pageable = PageRequest.of(page, size,
            Sort.by(Sort.Direction.fromString(sort.split(",")[1]), sort.split(",")[0]));
    Page<User> users = userService.findAll(pageable);
    return ResponseEntity.ok(users.map(userMapper::toResponse));
}

@GetMapping("/{id}")
public ResponseEntity<UserResponse> get(@PathVariable Long id,
                                        @RequestHeader(value = "If-None-Match", required = false) String etag) {
    return userService.findById(id)
            .map(u -> ResponseEntity.ok()
                    .eTag("\"" + u.hashCode() + "\"")
                    .body(userMapper.toResponse(u)))
            .orElse(ResponseEntity.notFound().build());
}
```

### Sequence Flow
```
Client -> HandlerMapping: GET /api/users/1
HandlerMapping -> Controller: invoke method
Controller -> Service: read data
Service -> Controller: entity
Controller -> HttpMessageConverter: serialize
HttpMessageConverter -> Client: 200 JSON
```

### Advanced Features
- **ETag/Last-Modified**: Conditional GETs
- **Pagination**: `Pageable`, `Slice`
- **Projection**: Sparse fieldsets or projections
- **Caching**: Response caching headers

---

## @PostMapping

### What
Shortcut for `@RequestMapping(method = RequestMethod.POST)` to create resources or trigger server-side actions.

### Why
- **Creation Semantics**: Resource creation with 201 + Location
- **Payload**: Submit JSON bodies safely
- **Validation**: Natural place for input validation

### Internal Working
1. **Argument Resolver** binds `@RequestBody`
2. **Validation** via `@Validated` and Bean Validation
3. **Service Layer** performs creation
4. **ResponseEntity** builds 201 with Location header

### Deep Dive
```java
@PostMapping
public ResponseEntity<UserResponse> create(@Validated @RequestBody UserRequest request) {
    User user = userService.createUser(request);
    return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/users/" + user.getId()))
            .body(userMapper.toResponse(user));
}
```

### Sequence Flow
```
Client -> Controller: POST /api/users (JSON)
Controller -> Validation: validate DTO
Validation -> Controller: OK
Controller -> Service: createUser
Service -> Repository: save
Repository -> Service: persisted entity
Service -> Controller: entity
Controller -> Client: 201 Created + Location
```

### Advanced Features
- **Idempotency-Key**: Deduplicate retries
- **Server-side generated IDs**: Return Location
- **Async processing**: 202 Accepted with polling

---

## @PutMapping

### What
Shortcut for `@RequestMapping(method = RequestMethod.PUT)` to replace resources fully.

### Why
- **Idempotent**: Safe to retry
- **Full Update**: Clear intent to replace state
- **Consistency**: Works well with ETag preconditions

### Internal Working
1. **Argument Resolver** binds `@RequestBody`
2. **Validation** for complete payloads
3. **Service** loads and updates entity
4. **Return** 200 with resource or 204 if no body

### Deep Dive
```java
@PutMapping("/{id}")
public ResponseEntity<UserResponse> replace(
        @PathVariable Long id,
        @Validated @RequestBody UserRequest request) {
    return userService.updateUser(id, request)
            .map(u -> ResponseEntity.ok(userMapper.toResponse(u)))
            .orElse(ResponseEntity.notFound().build());
}
```

### Sequence Flow
```
Client -> Controller: PUT /api/users/1 (JSON)
Controller -> Service: updateUser(id, dto)
Service -> Repository: load + save
Repository -> Service: updated entity
Service -> Controller: entity
Controller -> Client: 200 UserResponse
```

### Advanced Features
- **ETag If-Match**: Prevent lost updates
- **Partial failure**: Prefer PATCH for partial updates

---

## @DeleteMapping

### What
Shortcut for `@RequestMapping(method = RequestMethod.DELETE)` to delete resources.

### Why
- **Resource removal**: Clear semantics
- **Idempotent**: Multiple deletes are safe
- **RESTful**: Return 204 on success

### Internal Working
1. **Path binding** for resource id
2. **Service** deletes by id
3. **Return** 204 or 404 if not found

### Deep Dive
```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    boolean deleted = userService.deleteUser(id);
    return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
}
```

### Sequence Flow
```
Client -> Controller: DELETE /api/users/1
Controller -> Service: deleteUser(1)
Service -> Repository: deleteById
alt existed
  Repository -> Service: ok
  Service -> Controller: true
  Controller -> Client: 204 No Content
else missing
  Service -> Controller: false
  Controller -> Client: 404 Not Found
end
```

### Advanced Features
- **Soft delete**: flag-based deletion
- **Cascade rules**: DB-level constraints

---

## @PatchMapping

### What
Shortcut for `@RequestMapping(method = RequestMethod.PATCH)` to partially update resources.

### Why
- **Partial Updates**: Send only changed fields
- **Efficiency**: Smaller payloads
- **Semantics**: Non-idempotent by default (can be idempotent by design)

### Internal Working
1. **Argument Resolver** binds partial DTO or `Map<String,Object>`
2. **Validation** conditional per-field
3. **Service** merges changes and saves
4. **Return** updated resource

### Deep Dive
```java
@PatchMapping("/{id}")
public ResponseEntity<UserResponse> patch(@PathVariable Long id,
                                          @RequestBody Map<String, Object> updates) {
    return userService.patchUser(id, updates)
            .map(u -> ResponseEntity.ok(userMapper.toResponse(u)))
            .orElse(ResponseEntity.notFound().build());
}
```

### Sequence Flow
```
Client -> Controller: PATCH /api/users/1 (partial JSON)
Controller -> Service: patchUser(id, updates)
Service -> Repository: load entity
Service -> Entity: apply partial changes
Service -> Repository: save
Repository -> Service: updated entity
Service -> Controller: entity
Controller -> Client: 200 UserResponse
```

### Advanced Features
- **JSON Patch/Merge Patch**: RFC 6902/7386 support
- **Field-level validation**: groups/conditional validators
- **Conflict handling**: ETag preconditions

## Additional Web Layer Annotations

### @ControllerAdvice
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("User Not Found");
        problemDetail.setDetail(ex.getMessage());
        return ResponseEntity.notFound().body(problemDetail);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationErrors(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Validation Failed");
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage()));
        
        problemDetail.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problemDetail);
    }
}
```

#### Sequence Flow
```
Controller -> throws Exception: e.g., UserNotFoundException / MethodArgumentNotValidException
Exception -> @ControllerAdvice: locate @ExceptionHandler
@ControllerAdvice -> ResponseEntity<ProblemDetail>: build error response
ResponseEntity -> Client: appropriate 4xx/5xx with details
```

### @CrossOrigin
```java
@RestController
@CrossOrigin(origins = {"http://localhost:3000", "https://myapp.com"})
@RequestMapping("/api/users")
public class UserController {
    
    @CrossOrigin(origins = "https://admin.myapp.com")
    @GetMapping("/admin")
    public ResponseEntity<List<UserResponse>> getAdminUsers() {
        // Specific CORS for admin endpoint
    }
}
```

#### Sequence Flow
```
Client (Origin) -> CORS Filter: preflight/actual request
alt Origin allowed
  CORS Filter -> Controller: forward request
  Controller -> Response: normal handling
  CORS Filter -> Client: response with Access-Control-Allow-* headers
else Origin blocked
  CORS Filter -> Client: 403/blocked (no CORS headers)
end
```

### @RequestHeader
```java
@GetMapping("/{id}")
public ResponseEntity<UserResponse> getUser(
        @PathVariable Long id,
        @RequestHeader("X-User-ID") String userId,
        @RequestHeader("X-Request-ID") String requestId,
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String language) {
    
    // Access headers in method
}
```

#### Sequence Flow
```
Client -> Controller: include headers (X-User-ID, X-Request-ID, Accept-Language)
Controller -> Argument Resolver: bind headers to parameters
Controller -> Service: use header context
Service -> Controller: result
Controller -> Client: response
```

---

## Quick Reference & Best Practices

### HTTP Status Code Patterns
- **200 OK**: Successful GET, PUT, PATCH
- **201 Created**: Successful POST with resource creation
- **204 No Content**: Successful DELETE, PUT without body
- **400 Bad Request**: Validation errors, malformed requests
- **401 Unauthorized**: Authentication required
- **403 Forbidden**: Insufficient permissions
- **404 Not Found**: Resource doesn't exist
- **409 Conflict**: Business rule violations
- **422 Unprocessable Entity**: Semantic errors
- **500 Internal Server Error**: Unexpected server errors

### Validation Flow
```
@Validated → Bean Validation → ConstraintViolationException → 
MethodArgumentNotValidException → @ControllerAdvice → 400 Problem Details
```

### REST Best Practices
- Use proper HTTP methods (GET, POST, PUT, DELETE, PATCH)
- Return appropriate status codes
- Use DTOs for API boundaries
- Implement proper error handling
- Add comprehensive logging
- Use OpenAPI/Swagger for documentation
- Implement proper security (authentication/authorization)
- Use pagination for large datasets
- Support content negotiation
- Follow RESTful URL conventions

### Performance Considerations
- Use `@Transactional(readOnly = true)` for queries
- Implement proper caching strategies
- Use pagination for large result sets
- Optimize database queries
- Implement proper logging levels
- Use connection pooling
- Monitor response times
- Implement circuit breakers for external calls
