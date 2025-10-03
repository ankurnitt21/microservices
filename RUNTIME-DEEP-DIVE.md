# Bean Order: Startup vs API Hit

---

## **1️⃣ WHEN APP STARTS (Bean Creation Order)**

```
ORDER  BEAN NAME                              WHAT IT IS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1.     ApplicationContext                     IoC Container
2.     Environment                            Loads application.properties
3.     BeanFactory                            Core bean factory
4.     AutowiredAnnotationBeanPostProcessor   Handles @Autowired

DATA LAYER BEANS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
5.     HikariDataSource                       DB connection pool
6.     EntityManagerFactory                   Hibernate/JPA factory
7.     JpaTransactionManager                  Transaction manager
8.     UserRepository                         JPA proxy (wraps SimpleJpaRepository)

BUSINESS LAYER BEANS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
9.     UserService                            TX proxy wrapping your service

WEB FOUNDATION BEANS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
10.    ObjectMapper                           Jackson JSON processor
11.    MappingJackson2HttpMessageConverter    JSON ↔ Java converter
12.    LocalValidatorFactoryBean              Bean Validation engine
13.    ConversionService                      Type conversions (String → int)

WEB MVC BEANS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
14.    RequestMappingHandlerMapping           Maps URLs to controller methods
15.    RequestMappingHandlerAdapter           Invokes controller methods
16.    UserController                         Your REST controller
17.    GlobalExceptionHandler                 @ControllerAdvice for errors
18.    ExceptionHandlerExceptionResolver      Finds exception handlers

SECURITY BEANS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
19.    SecurityFilterChain                    Security rules
20.    FilterChainProxy                       Manages security filters
21.    JwtDecoder                             JWT token decoder
22.    PasswordEncoder                        Password hashing

WEB SERVER BEANS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
23.    DispatcherServlet                      Front controller
24.    TomcatWebServer                        Embedded server

✅ APP READY - All 24 beans created and ready
```

---

## **2️⃣ WHEN GET API HIT (Bean Call Order)**

**Request: `GET /api/users?page=0&size=10&sort=createdAt`**

```
STEP   BEAN CALLED                            WHAT IT DOES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
       [HTTP Request arrives at Tomcat]

1.     FilterChainProxy                       Runs security filters
                                              JWT validation, authentication

2.     DispatcherServlet                      Front controller receives request

3.     RequestMappingHandlerMapping           Finds handler method
                                              → UserController#getAllUsers

4.     RequestMappingHandlerAdapter           Prepares to invoke method
                                              Uses argument resolvers

       [ConversionService]                    Converts query params:
                                              "0" → int 0
                                              "10" → int 10

5.     UserController                         Method: getAllUsers(0, 10, "createdAt")
                                              Validates params
                                              Creates PageRequest

6.     UserService                            Method: findAll(pageable)
       (TX proxy)                             Opens read-only transaction

7.     JpaTransactionManager                  Starts transaction

8.     UserRepository                         Method: findAll(pageable)
       (JPA proxy)                            Generates SQL

9.     EntityManagerFactory                   Executes SQL via Hibernate

10.    HikariDataSource                       Gets DB connection
                                              Runs: SELECT * FROM users...

       [Results flow back up the chain]

11.    UserController                         Maps Page<User> → Page<UserResponse>
                                              Returns response

12.    MappingJackson2HttpMessageConverter    Serializes to JSON

       [HTTP Response sent: 200 OK + JSON]
```

---

## **3️⃣ WHEN POST API HIT (Bean Call Order)**

**Request: `POST /api/users` with JSON body**

```
STEP   BEAN CALLED                            WHAT IT DOES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
       [HTTP Request with JSON body arrives]

1.     FilterChainProxy                       Security filters

2.     DispatcherServlet                      Receives request

3.     RequestMappingHandlerMapping           Finds: UserController#addUser

4.     RequestMappingHandlerAdapter           Prepares method invocation

5.     MappingJackson2HttpMessageConverter    Deserializes JSON → UserRequest

6.     LocalValidatorFactoryBean              Validates @Validated(Create.class)
                                              Checks: name, email constraints
                                              ❌ If invalid → Exception → [13]

7.     UserController                         Method: addUser(userRequest)

8.     UserService                            Method: create(request)
       (TX proxy)                             

9.     JpaTransactionManager                  Opens write transaction

10.    UserRepository                         existsByEmailIgnoreCase(email)
       (JPA proxy)                            SQL: SELECT COUNT(*)...
                                              
                                              ❌ If exists → Exception → [13]
                                              ✅ If not exists → save(user)
                                              SQL: INSERT INTO users...

11.    JpaTransactionManager                  Commits transaction

12.    UserController                         Returns ResponseEntity<UserResponse>

13.    MappingJackson2HttpMessageConverter    Serializes to JSON

       [HTTP Response: 201 Created + JSON]
```

---

## **4️⃣ WHEN ERROR OCCURS (Bean Call Order)**

```
STEP   BEAN CALLED                            WHAT IT DOES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
       [Exception thrown somewhere in flow]

1.     ExceptionHandlerExceptionResolver      Catches exception
                                              Searches for @ExceptionHandler

2.     GlobalExceptionHandler                 Finds matching handler method:
                                              - handleMethodArgumentNotValid()
                                              - handleIllegalState()
                                              Creates ProblemDetail

3.     MappingJackson2HttpMessageConverter    Serializes error to JSON

       [HTTP Response: 400/409/500 + JSON]
```

---

## **5️⃣ VISUAL COMPARISON**

### **Startup (Once)**
```
ApplicationContext
    ↓
DataSource → EntityManagerFactory → TransactionManager
    ↓
Repository (proxy)
    ↓
Service (TX proxy)
    ↓
Controller
    ↓
DispatcherServlet + Security + Server

⏱️ Takes ~2 seconds
✅ Beans stored in memory
```

### **Every API Request (Reuses Beans)**
```
FilterChainProxy (security)
    ↓
DispatcherServlet
    ↓
HandlerMapping → HandlerAdapter
    ↓
Controller → Service → Repository
    ↓
DataSource (gets connection)
    ↓
Response via MessageConverter

⏱️ Takes ~10-50ms
♻️ Same beans, different request data
```

---

## **Key Differences**

| Aspect | Startup | API Hit |
|--------|---------|---------|
| **Frequency** | Once | Every request |
| **Creates beans?** | Yes (24 beans) | No (reuses existing) |
| **Time taken** | 1-3 seconds | 10-100ms |
| **What's new** | Beans created | Request objects (UserRequest, PageRequest) |
| **Thread** | Main thread | Request thread (from pool) |
| **DB connection** | Connection pool created | Gets connection from pool |
| **Transaction** | N/A | Created and committed per request |

---

This shows the **exact order** both at startup and during request processing!

# What Happens to Beans After API Returns Response

---

## **🔄 AFTER RESPONSE IS SENT**

```
Client receives response (200 OK + JSON)
          ↓
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
CLEANUP PHASE - What happens to beans?
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

BEANS → NOTHING HAPPENS! They stay in memory

✓ UserController        - Still in memory, unchanged
✓ UserService           - Still in memory, unchanged  
✓ UserRepository        - Still in memory, unchanged
✓ DispatcherServlet     - Still in memory, unchanged
✓ All other beans       - Still in memory, unchanged

They are SINGLETON beans - created once at startup, 
reused forever, destroyed only at shutdown.
```

---

## **📊 WHAT ACTUALLY GETS CLEANED UP**

### **1. Request-Scoped Objects (NOT Beans)**
```
❌ DESTROYED after response:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✗ HttpServletRequest        - Garbage collected
✗ HttpServletResponse       - Garbage collected
✗ UserRequest (DTO)         - Garbage collected
✗ UserResponse (DTO)        - Garbage collected
✗ PageRequest               - Garbage collected
✗ Page<User> result         - Garbage collected
✗ SecurityContext           - Cleared from ThreadLocal
✗ Method call stack         - Unwound
✗ Local variables           - Gone
```

### **2. Database Resources**
```
🔌 RETURNED TO POOL:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
↻ DB Connection             - Returned to HikariDataSource pool
                              (NOT closed, just available for reuse)

↻ EntityManager             - Closed (thread-local, per-request)
                              (But EntityManagerFactory bean stays)

↻ Transaction               - Committed and completed
                              (But JpaTransactionManager bean stays)
```

### **3. Thread Resources**
```
🧵 THREAD CLEANUP:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
↻ Request Thread            - Returned to Tomcat thread pool
                              (Ready to handle next request)

↻ SecurityContext           - Cleared from thread:
                              SecurityContextHolder.clearContext()

↻ ThreadLocal variables     - Cleared to prevent memory leaks
```

---

## **🔁 COMPLETE LIFECYCLE AFTER RESPONSE**

```
TIMELINE: What happens after "200 OK" is sent
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Time    Event                              What Happens to Beans
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
0ms     Response sent to client            Beans: UNCHANGED

1ms     Transaction committed              JpaTransactionManager: UNCHANGED
                                           (Just managed the transaction)

2ms     EntityManager closed               EntityManagerFactory: UNCHANGED
                                           (Just provided the EntityManager)

3ms     DB connection returned             HikariDataSource: UNCHANGED
                                           (Connection back in pool)

4ms     SecurityContext cleared            FilterChainProxy: UNCHANGED
                                           (Just cleaned ThreadLocal)

5ms     Thread available for next request  All beans: UNCHANGED
                                           (Thread reused, beans reused)

Request objects garbage collected:         All beans: STILL IN MEMORY
- UserRequest, UserResponse, PageRequest   Ready for next request!
- HttpServletRequest/Response
- Local variables, call stack
```

---

## **💾 BEAN STATE AFTER REQUEST**

### **Beans DO NOT Store Request Data**
```java
// ❌ BAD - This would cause problems (but we don't do this)
@Service
public class UserService {
    private UserRequest currentRequest;  // ❌ NEVER DO THIS!
    // This would mix data between requests
}

// ✅ GOOD - Beans are stateless
@Service
public class UserService {
    private final UserRepository repository;  // ✅ Only dependencies
    
    // Method parameters hold request data (stack-scoped)
    public User create(UserRequest request) {  // ✅ Parameter, not field
        // request exists only during this method call
    }
}
```

### **Bean Memory State**
```
BEFORE REQUEST:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
UserService bean:
  - repository field → points to UserRepository bean
  - No request data stored

DURING REQUEST:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
UserService bean:
  - repository field → points to UserRepository bean
  - create() method executing with parameters on call stack

AFTER REQUEST:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
UserService bean:
  - repository field → points to UserRepository bean
  - No request data stored (parameters garbage collected)

EXACTLY THE SAME! ✅
```

---

## **🔄 NEXT REQUEST (Same Beans Reused)**

```
New request arrives: GET /api/users?page=1&size=20
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. FilterChainProxy       ← SAME BEAN (reused)
2. DispatcherServlet       ← SAME BEAN (reused)
3. HandlerMapping          ← SAME BEAN (reused)
4. UserController          ← SAME BEAN (reused)
5. UserService             ← SAME BEAN (reused)
6. UserRepository          ← SAME BEAN (reused)
7. HikariDataSource        ← SAME BEAN (reused, provides connection)

NEW request-scoped objects:
- Different HttpServletRequest
- Different PageRequest(page=1, size=20)
- Different Page<User> result
- Different thread (maybe, from pool)
```

---

## **📈 CONCURRENT REQUESTS**

```
Multiple requests at the same time:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Request 1 (Thread A):                Request 2 (Thread B):
GET /api/users?page=0                 POST /api/users
    ↓                                     ↓
UserController bean ←────────────────────┤
    (same instance)                       │
    ↓                                     ↓
UserService bean ←───────────────────────┤
    (same instance)                       │
    ↓                                     ↓
UserRepository bean ←────────────────────┘
    (same instance)

🔒 Thread-safe because:
- Beans are stateless (no shared mutable state)
- Request data on separate thread stacks
- DB connections from pool (separate for each thread)
- Transactions isolated per thread
```

---

## **🗑️ WHEN BEANS ARE ACTUALLY DESTROYED**

**Beans are destroyed ONLY when application shuts down:**

```
APPLICATION SHUTDOWN SEQUENCE:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. Server stops accepting new requests
2. In-flight requests complete
3. @PreDestroy methods called on beans
4. Beans destroyed in reverse dependency order:
   
   TomcatWebServer          → destroyed
   DispatcherServlet        → destroyed
   UserController           → destroyed
   GlobalExceptionHandler   → destroyed
   UserService              → destroyed
   UserRepository           → destroyed
   JpaTransactionManager    → destroyed
   EntityManagerFactory     → closed
   HikariDataSource         → pool closed, connections closed
   ApplicationContext       → closed

5. JVM exits
```

---

## **📊 MEMORY DIAGRAM**

```
HEAP MEMORY (Stays until shutdown):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
┌─────────────────────────────────────────────────────┐
│ ApplicationContext (IoC Container)                   │
│                                                       │
│  ┌──────────────────┐  ┌──────────────────┐        │
│  │ UserController   │  │ UserService      │        │
│  │ (singleton)      │  │ (TX proxy)       │        │
│  │ - userService ───┼─→│ - repository ────┼──┐    │
│  └──────────────────┘  └──────────────────┘  │    │
│                                               ↓    │
│  ┌──────────────────┐  ┌──────────────────┐  │    │
│  │ DataSource       │  │ UserRepository   │←─┘    │
│  │ (pool)           │  │ (JPA proxy)      │        │
│  └──────────────────┘  └──────────────────┘        │
│                                                     │
│  ... 20 more beans ...                             │
└─────────────────────────────────────────────────────┘

THREAD STACK (Cleared after each request):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
┌─────────────────────────────────────────────────────┐
│ Thread-1 (http-nio-8080-exec-1)                     │
│                                                      │
│ During Request:                 After Request:      │
│ ├─ HttpServletRequest           ├─ (empty)         │
│ ├─ UserRequest                  ├─ (empty)         │
│ ├─ page=0, size=10              ├─ (empty)         │
│ ├─ PageRequest                  ├─ (empty)         │
│ ├─ Page<User>                   ├─ (empty)         │
│ └─ SecurityContext              └─ (cleared)       │
│                                                     │
│ Thread returned to pool, ready for next request    │
└─────────────────────────────────────────────────────┘
```

---

## **✅ SUMMARY**

| Component | After Response |
|-----------|---------------|
| **Beans** | ✅ Stay in memory unchanged |
| **Request objects** | ❌ Garbage collected |
| **DB Connection** | ↻ Returned to pool |
| **Transaction** | ✅ Committed and closed |
| **Thread** | ↻ Returned to thread pool |
| **SecurityContext** | ❌ Cleared from ThreadLocal |
| **Method stack** | ❌ Unwound |

**Key Point:** Beans live forever (until shutdown). Only request-specific data is cleaned up. This is why Spring Boot can handle thousands of requests per second - beans are reused, not recreated!