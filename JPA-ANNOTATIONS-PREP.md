# Spring Data JPA - Complete Interview Prep Guide

Comprehensive notes on Spring Data JPA covering fundamentals to advanced topics with examples and use cases.

---

## 1. Introduction & Setup

### Spring Data JPA Overview
- **What**: Abstraction layer over JPA that reduces boilerplate code for data access.
- **Why**: Eliminates need to write repository implementations; provides query derivation, pagination, auditing.
- **How**: Uses JPA/Hibernate under the hood; adds Spring magic for repositories.
- **Use Case**: Rapid development of data access layers in Spring Boot applications.

### Dependencies & Configuration
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

```properties
# application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
```

---

## 2. Entity Mapping Fundamentals

### @Entity
- **What**: Marks a class as a JPA entity mapped to a database table.
- **Why**: Enables ORM mapping and persistence lifecycle management.
- **How**: JPA provider creates table mapping and manages entity lifecycle.
- **Use Case**: Domain objects that need persistence (User, Order, Product).

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_name", nullable = false, length = 255)
    private String name;
    
    @Column(name = "user_email", unique = true, nullable = false)
    private String email;
}
```

### @Table
- **What**: Configures table name, schema, constraints, and indexes.
- **Why**: Control physical table structure and database constraints.
- **How**: Affects DDL generation and database schema.
- **Use Case**: Multi-tenant apps (schema), performance optimization (indexes).

```java
@Entity
@Table(
    name = "orders",
    schema = "ecommerce",
    uniqueConstraints = @UniqueConstraint(name = "uk_order_number", columnNames = "order_number"),
    indexes = @Index(name = "idx_order_date", columnList = "order_date")
)
public class Order { ... }
```

### @Id & @GeneratedValue
- **What**: Defines primary key and generation strategy.
- **Why**: Ensure unique entity identity; avoid manual key management.
- **How**: JPA tracks entities by ID; generation strategy determines key creation.
- **Use Case**: All entities need unique identifiers.

```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
@SequenceGenerator(name = "user_seq", sequenceName = "user_seq", allocationSize = 50)
private Long id;
```

**Strategies:**
- `IDENTITY`: Auto-increment (MySQL, SQL Server)
- `SEQUENCE`: Database sequence (PostgreSQL, Oracle)
- `TABLE`: Table-based sequence (portable but slower)
- `AUTO`: Provider chooses

### @Column
- **What**: Maps field to column with constraints and metadata.
- **Why**: Control column properties, validation, and database constraints.
- **How**: Affects DDL generation and runtime validation.
- **Use Case**: Data integrity, performance optimization.

```java
@Column(name = "user_email", nullable = false, unique = true, length = 320)
private String email;

@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;
```

### @Transient, @Lob, @Basic
- **@Transient**: Field not persisted to database.
- **@Lob**: Large objects (CLOB/BLOB) for text/binary data.
- **@Basic**: Basic field mapping with fetch hints.

```java
@Transient
private String fullName; // computed field

@Lob
private String description; // large text

@Basic(fetch = FetchType.LAZY)
private String notes; // lazy loading hint
```

---

## 3. Relationships

### @OneToOne
- **What**: One-to-one association between entities.
- **Why**: Model 1:1 relationships (User-Profile, Order-Payment).
- **How**: Uses foreign key or shared primary key.
- **Use Case**: User profiles, payment details, shipping addresses.

```java
@Entity
public class User {
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserProfile profile;
}

@Entity
public class UserProfile {
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
}
```

### @OneToMany / @ManyToOne
- **What**: Parent-child relationships (1:N, N:1).
- **Why**: Model hierarchical data (User-Orders, Category-Products).
- **How**: Foreign key in child table references parent.
- **Use Case**: Orders per user, products per category.

```java
@Entity
public class User {
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();
}

@Entity
public class Order {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
```

### @ManyToMany
- **What**: Many-to-many relationships via join table.
- **Why**: Model many-to-many associations (User-Roles, Product-Categories).
- **How**: Uses intermediate join table with foreign keys.
- **Use Case**: User roles, product tags, course enrollments.

```java
@Entity
public class User {
    @ManyToMany
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}
```

### Cascade Types
- **PERSIST**: Save parent saves children.
- **MERGE**: Update parent updates children.
- **REMOVE**: Delete parent deletes children.
- **ALL**: All operations cascade.
- **orphanRemoval=true**: Delete children removed from collection.

---

## 4. Inheritance Strategies

### @Inheritance
- **What**: Maps inheritance hierarchies to database tables.
- **Why**: Model polymorphic entities with shared/different attributes.
- **How**: Three strategies for mapping inheritance to tables.
- **Use Case**: Payment types, product categories, user types.

```java
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "payment_type")
public abstract class Payment {
    @Id
    private Long id;
    private BigDecimal amount;
}

@Entity
@DiscriminatorValue("CARD")
public class CardPayment extends Payment {
    private String cardNumber;
    private String expiryDate;
}
```

**Strategies:**
- `SINGLE_TABLE`: One table with discriminator column (default).
- `JOINED`: Separate tables with joins.
- `TABLE_PER_CLASS`: One table per concrete class.

---

## 5. Repository Interfaces

### Repository Hierarchy
```java
// Basic marker interface
public interface UserRepository extends Repository<User, Long> { }

// CRUD operations
public interface UserRepository extends CrudRepository<User, Long> { }

// CRUD + pagination/sorting
public interface UserRepository extends PagingAndSortingRepository<User, Long> { }

// Full JPA features
public interface UserRepository extends JpaRepository<User, Long> { }
```

### Query Method Derivation
- **What**: Generate queries from method names.
- **Why**: Reduce boilerplate; type-safe queries.
- **How**: Spring Data parses method names into JPQL.
- **Use Case**: Simple queries without custom implementation.

```java
public interface UserRepository extends JpaRepository<User, Long> {
    // Find by property
    List<User> findByName(String name);
    Optional<User> findByEmail(String email);
    
    // Multiple conditions
    List<User> findByNameAndEmail(String name, String email);
    List<User> findByNameOrEmail(String name, String email);
    
    // Comparison operators
    List<User> findByAgeGreaterThan(int age);
    List<User> findByAgeBetween(int min, int max);
    List<User> findByCreatedAtAfter(LocalDateTime date);
    
    // Pattern matching
    List<User> findByNameContaining(String name);
    List<User> findByNameStartingWith(String prefix);
    List<User> findByNameEndingWith(String suffix);
    
    // Collection operations
    List<User> findByIdIn(Collection<Long> ids);
    List<User> findByRolesName(String roleName);
    
    // Sorting and limiting
    List<User> findTop10ByOrderByCreatedAtDesc();
    List<User> findFirst5ByNameContainingOrderByNameAsc(String name);
    
    // Count and exists
    long countByEmail(String email);
    boolean existsByEmail(String email);
    
    // Delete operations
    void deleteByEmail(String email);
    long deleteByCreatedAtBefore(LocalDateTime date);
}
```

---

## 6. Custom Queries

### @Query (JPQL)
- **What**: Define custom JPQL queries.
- **Why**: Complex queries not expressible via method names.
- **How**: JPQL operates on entities, not tables.
- **Use Case**: Complex business logic, performance optimization.

```java
@Query("SELECT u FROM User u WHERE u.email = :email AND u.active = true")
Optional<User> findActiveUserByEmail(@Param("email") String email);

@Query("SELECT u FROM User u JOIN u.orders o WHERE o.status = :status")
List<User> findUsersWithOrdersByStatus(@Param("status") OrderStatus status);

@Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :start AND :end")
List<User> findUsersCreatedBetween(@Param("start") LocalDateTime start, 
                                  @Param("end") LocalDateTime end);
```

### @Query (Native SQL)
- **What**: Execute native SQL queries.
- **Why**: Database-specific features, performance-critical queries.
- **How**: Direct SQL execution with result mapping.
- **Use Case**: Complex reporting, database-specific functions.

```java
@Query(value = "SELECT * FROM users WHERE user_email = :email", nativeQuery = true)
Optional<User> findByEmailNative(@Param("email") String email);

@Query(value = "SELECT u.*, COUNT(o.id) as order_count FROM users u " +
               "LEFT JOIN orders o ON u.id = o.user_id " +
               "GROUP BY u.id", nativeQuery = true)
List<Object[]> findUsersWithOrderCounts();
```

### @Modifying Queries
- **What**: Update/delete queries that modify data.
- **Why**: Bulk operations, data cleanup.
- **How**: Requires @Transactional; clears persistence context.
- **Use Case**: Bulk updates, data migration, cleanup operations.

```java
@Modifying
@Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.email = :email")
int updateLastLogin(@Param("email") String email, @Param("loginTime") LocalDateTime loginTime);

@Modifying
@Query("DELETE FROM User u WHERE u.createdAt < :cutoffDate")
int deleteInactiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate);
```

---

## 7. Pagination & Sorting

### Pageable & Page
- **What**: Pagination and sorting for large datasets.
- **Why**: Performance, user experience, memory management.
- **How**: LIMIT/OFFSET with ORDER BY in SQL.
- **Use Case**: Data tables, search results, API endpoints.

```java
// Repository method
Page<User> findByNameContaining(String name, Pageable pageable);

// Service usage
public Page<UserResponse> searchUsers(String name, int page, int size, String sort) {
    Sort.Direction direction = sort.endsWith("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
    String sortBy = sort.split(",")[0];
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
    
    Page<User> users = userRepository.findByNameContaining(name, pageable);
    return users.map(this::toUserResponse);
}

// Controller
@GetMapping("/users")
public Page<UserResponse> getUsers(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(defaultValue = "name,asc") String sort) {
    return userService.searchUsers("", page, size, sort);
}
```

### Slice Interface
- **What**: Pagination without total count (faster).
- **Why**: Performance when total count not needed.
- **How**: Only fetches next page indicator.
- **Use Case**: Infinite scroll, real-time feeds.

```java
Slice<User> findByNameContaining(String name, Pageable pageable);
```

---

## 8. Specifications & Criteria API

### JPA Specifications
- **What**: Dynamic query building using Criteria API.
- **Why**: Complex dynamic queries, reusable query components.
- **How**: Programmatic query construction with predicates.
- **Use Case**: Advanced search, filtering, dynamic reports.

```java
public class UserSpecifications {
    public static Specification<User> hasName(String name) {
        return (root, query, cb) -> 
            name == null ? null : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }
    
    public static Specification<User> hasEmail(String email) {
        return (root, query, cb) -> 
            email == null ? null : cb.equal(root.get("email"), email);
    }
    
    public static Specification<User> isActive() {
        return (root, query, cb) -> cb.equal(root.get("active"), true);
    }
}

// Usage
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> { }

// Service
public List<User> searchUsers(String name, String email, boolean activeOnly) {
    Specification<User> spec = Specification.where(UserSpecifications.hasName(name))
        .and(UserSpecifications.hasEmail(email));
    
    if (activeOnly) {
        spec = spec.and(UserSpecifications.isActive());
    }
    
    return userRepository.findAll(spec);
}
```

---

## 9. Projections

### Interface-based Projections
- **What**: Expose subset of entity fields.
- **Why**: Performance, security, API design.
- **How**: Interface methods map to entity properties.
- **Use Case**: API responses, performance optimization.

```java
// Closed projection
public interface UserSummary {
    Long getId();
    String getName();
    String getEmail();
}

// Open projection with @Value
public interface UserWithFullName {
    @Value("#{target.name + ' ' + target.lastName}")
    String getFullName();
    
    String getEmail();
}

// Usage
List<UserSummary> findByNameContaining(String name);
```

### Class-based Projections (DTOs)
- **What**: Constructor-based projections to DTOs.
- **Why**: Type safety, validation, complex mappings.
- **How**: Constructor parameters map to query results.
- **Use Case**: API DTOs, complex data transformation.

```java
public class UserDto {
    private final Long id;
    private final String name;
    private final String email;
    
    public UserDto(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
    // getters...
}

// Repository
@Query("SELECT new com.example.dto.UserDto(u.id, u.name, u.email) FROM User u WHERE u.active = true")
List<UserDto> findActiveUsersAsDto();
```

---

## 10. Auditing

### Spring Data JPA Auditing
- **What**: Automatic tracking of creation/modification metadata.
- **Why**: Audit trails, compliance, debugging.
- **How**: Entity listeners and Spring Security integration.
- **Use Case**: Audit logs, user tracking, data governance.

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    private Long id;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    @CreatedBy
    private String createdBy;
    
    @LastModifiedBy
    private String updatedBy;
}

// Configuration
@EnableJpaAuditing
@Configuration
public class JpaConfig {
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getName);
    }
}
```

---

## 11. Transactions

### @Transactional
- **What**: Declares transactional boundaries.
- **Why**: Data consistency, ACID properties.
- **How**: Spring AOP creates proxies for transaction management.
- **Use Case**: Multi-step operations, data integrity.

```java
@Service
@Transactional
public class UserService {
    
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    @Transactional
    public User createUser(UserRequest request) {
        // Validation
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        // Create user
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        
        return userRepository.save(user);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void transferUserData(Long fromUserId, Long toUserId) {
        // Complex multi-step operation
        // Automatic rollback on any exception
    }
}
```

**Transaction Attributes:**
- `propagation`: How transactions interact (REQUIRED, REQUIRES_NEW, etc.)
- `isolation`: Data visibility (READ_COMMITTED, REPEATABLE_READ, etc.)
- `readOnly`: Optimization hint for read-only operations
- `rollbackFor`: Exceptions that trigger rollback
- `noRollbackFor`: Exceptions that don't trigger rollback

---

## 12. Entity Lifecycle & Callbacks

### Entity States
- **Transient**: Not associated with persistence context
- **Persistent**: Managed by persistence context
- **Detached**: Previously persistent, no longer managed
- **Removed**: Marked for deletion

### Lifecycle Callbacks
- **What**: Methods called during entity lifecycle events.
- **Why**: Business logic, validation, logging, cleanup.
- **How**: JPA invokes annotated methods at specific points.
- **Use Case**: Audit trails, validation, business rules.

```java
@Entity
public class User {
    @Id
    private Long id;
    private String name;
    private String email;
    private LocalDateTime lastLoginAt;
    
    @PrePersist
    public void prePersist() {
        if (this.email != null) {
            this.email = this.email.toLowerCase();
        }
        System.out.println("Creating user: " + this.name);
    }
    
    @PostPersist
    public void postPersist() {
        System.out.println("User created with ID: " + this.id);
    }
    
    @PreUpdate
    public void preUpdate() {
        this.lastLoginAt = LocalDateTime.now();
        System.out.println("Updating user: " + this.name);
    }
    
    @PostLoad
    public void postLoad() {
        System.out.println("User loaded: " + this.name);
    }
    
    @PreRemove
    public void preRemove() {
        System.out.println("Deleting user: " + this.name);
    }
}
```

---

## 13. Locking

### Optimistic Locking (@Version)
- **What**: Version-based conflict detection.
- **Why**: Prevent lost updates in concurrent environments.
- **How**: Version field incremented on updates; conflicts throw OptimisticLockException.
- **Use Case**: High-concurrency applications, collaborative editing.

```java
@Entity
public class User {
    @Id
    private Long id;
    
    @Version
    private Long version;
    
    private String name;
    private String email;
}

// Service
@Transactional
public User updateUser(Long id, UserRequest request) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("User not found"));
    
    user.setName(request.getName());
    user.setEmail(request.getEmail());
    
    try {
        return userRepository.save(user);
    } catch (OptimisticLockException e) {
        throw new ConflictException("User was modified by another user");
    }
}
```

### Pessimistic Locking
- **What**: Database-level locking during transaction.
- **Why**: Prevent concurrent access to critical data.
- **How**: Acquires locks at database level.
- **Use Case**: Financial transactions, inventory management.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT u FROM User u WHERE u.id = :id")
Optional<User> findByIdForUpdate(@Param("id") Long id);

// Usage
@Transactional
public void transferBalance(Long fromUserId, Long toUserId, BigDecimal amount) {
    User fromUser = userRepository.findByIdForUpdate(fromUserId)
        .orElseThrow(() -> new EntityNotFoundException("From user not found"));
    
    User toUser = userRepository.findByIdForUpdate(toUserId)
        .orElseThrow(() -> new EntityNotFoundException("To user not found"));
    
    // Transfer logic with locked entities
}
```

---

## 14. Caching

### First-level Cache (Persistence Context)
- **What**: EntityManager-level cache for current transaction.
- **Why**: Performance, consistency within transaction.
- **How**: Automatic caching by JPA provider.
- **Use Case**: Repeated queries within same transaction.

### Second-level Cache
- **What**: Application-wide cache shared across EntityManagers.
- **Why**: Performance across transactions, reduced database load.
- **How**: Cache provider (EhCache, Hazelcast) with entity annotations.
- **Use Case**: Read-heavy applications, reference data.

```java
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class User {
    @Id
    private Long id;
    private String name;
    private String email;
}

// Configuration
@EnableCaching
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("users");
    }
}
```

---

## 15. Performance Optimization

### N+1 Query Problem
- **What**: Lazy loading causes multiple queries for collections.
- **Why**: Default lazy loading loads collections one by one.
- **How**: Use fetch joins, entity graphs, or batch fetching.
- **Use Case**: Loading entities with related data.

```java
// Problem: N+1 queries
List<User> users = userRepository.findAll();
users.forEach(user -> user.getOrders().size()); // Triggers N queries

// Solution 1: Fetch join
@Query("SELECT u FROM User u LEFT JOIN FETCH u.orders")
List<User> findAllWithOrders();

// Solution 2: Entity graph
@EntityGraph(attributePaths = {"orders"})
List<User> findAll();

// Solution 3: Batch fetching
@BatchSize(size = 50)
@OneToMany(mappedBy = "user")
private List<Order> orders;
```

### Entity Graphs
- **What**: Define fetch plans for related entities.
- **Why**: Control what related data to load in single query.
- **How**: @EntityGraph annotation with attribute paths.
- **Use Case**: Complex entity loading, performance optimization.

```java
@Entity
@NamedEntityGraph(
    name = "User.withOrders",
    attributeNodes = @NamedAttributeNode("orders")
)
public class User { ... }

// Usage
@EntityGraph("User.withOrders")
List<User> findAll();
```

---

## 16. Testing

### @DataJpaTest
- **What**: Slice test for JPA components.
- **Why**: Fast, focused testing of data layer.
- **How**: In-memory database, auto-configuration of JPA components.
- **Use Case**: Repository testing, entity mapping validation.

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void shouldFindUserByEmail() {
        // Given
        User user = new User("John", "john@example.com");
        userRepository.save(user);
        
        // When
        Optional<User> found = userRepository.findByEmail("john@example.com");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John");
    }
    
    @Test
    void shouldReturnEmptyWhenUserNotFound() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");
        assertThat(found).isEmpty();
    }
}
```

### Testcontainers Integration
```java
@SpringBootTest
@Testcontainers
class UserServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

---

## 17. Best Practices & Common Pitfalls

### Design Patterns
```java
// Repository Pattern
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}

// Service Layer
@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    
    public UserResponse createUser(UserRequest request) {
        User user = userMapper.toEntity(request);
        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }
}

// DTO Pattern
public class UserRequest {
    @NotBlank
    private String name;
    
    @Email
    private String email;
}

public class UserResponse {
    private Long id;
    private String name;
    private String email;
}
```

### Common Pitfalls
1. **LazyInitializationException**: Access lazy collections outside transaction
2. **N+1 Queries**: Use fetch joins or entity graphs
3. **Transaction Boundaries**: Keep transactions short, avoid remote calls
4. **Memory Leaks**: Clear persistence context, use pagination
5. **Entity Exposure**: Use DTOs for API boundaries

### Performance Tips
- Use `@Transactional(readOnly = true)` for queries
- Implement proper indexing strategy
- Use projections for large result sets
- Consider caching for read-heavy scenarios
- Monitor query execution plans
- Use batch operations for bulk updates

---

## 18. Advanced Topics

### Multi-tenancy
```java
@Entity
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = "string"))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class User {
    @Id
    private Long id;
    
    @Column(name = "tenant_id")
    private String tenantId;
}
```

### Soft Deletes
```java
@Entity
@SQLDelete(sql = "UPDATE users SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class User {
    @Id
    private Long id;
    
    @Column(name = "deleted")
    private boolean deleted = false;
}
```

### Custom Repository Implementation
```java
public interface UserRepositoryCustom {
    List<User> findUsersWithComplexCriteria(SearchCriteria criteria);
}

@Repository
public class UserRepositoryImpl implements UserRepositoryCustom {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public List<User> findUsersWithComplexCriteria(SearchCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> root = query.from(User.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        if (criteria.getName() != null) {
            predicates.add(cb.like(root.get("name"), "%" + criteria.getName() + "%"));
        }
        
        if (criteria.getEmail() != null) {
            predicates.add(cb.equal(root.get("email"), criteria.getEmail()));
        }
        
        query.where(predicates.toArray(new Predicate[0]));
        return entityManager.createQuery(query).getResultList();
    }
}
```

---

## Quick Reference

### Key Annotations Summary
- **Entity Mapping**: `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`
- **Relationships**: `@OneToOne`, `@OneToMany`, `@ManyToOne`, `@ManyToMany`, `@JoinColumn`
- **Queries**: `@Query`, `@Modifying`, `@NamedQuery`
- **Lifecycle**: `@PrePersist`, `@PostPersist`, `@PreUpdate`, `@PostUpdate`
- **Locking**: `@Version`, `@Lock`
- **Caching**: `@Cacheable`, `@Cache`
- **Auditing**: `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy`

### Common Query Patterns
```java
// Find by property
List<User> findByName(String name);

// Multiple conditions
List<User> findByNameAndEmail(String name, String email);

// Comparison operators
List<User> findByAgeGreaterThan(int age);

// Pattern matching
List<User> findByNameContaining(String name);

// Pagination
Page<User> findByNameContaining(String name, Pageable pageable);

// Custom JPQL
@Query("SELECT u FROM User u WHERE u.active = true")
List<User> findActiveUsers();

// Native SQL
@Query(value = "SELECT * FROM users WHERE email = ?1", nativeQuery = true)
Optional<User> findByEmailNative(String email);
```

This comprehensive guide covers all major aspects of Spring Data JPA from fundamentals to advanced topics, providing practical examples and real-world use cases for interview preparation.

