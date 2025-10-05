# Spring Core Syllabus

## 1. IoC & Dependency Injection

### Step 1: Without DI (Traditional Approach)

```java
// PlayStation class
public class PlayStation {
    public void startGame() {
        System.out.println("PlayStation started!");
    }
}

// Gamer class - creates PlayStation itself
public class Gamer {
    private PlayStation playStation;
    
    public Gamer() {
        // PROBLEM: Gamer creates PlayStation directly
        this.playStation = new PlayStation();
    }
    
    public void playGame() {
        playStation.startGame();
        System.out.println("Gamer is playing!");
    }
}

// Main class
public class TraditionalApp {
    public static void main(String[] args) {
        Gamer gamer = new Gamer();
        gamer.playGame();
    }
}
```

**What's happening here:**
- Gamer is responsible for creating PlayStation
- This creates **tight coupling** - Gamer is tightly bound to PlayStation
- If you want to use Xbox instead, you have to change Gamer code

**Problems:**
- **Tight coupling:** Gamer can't use Xbox
- **Hard to test:** Can't easily mock PlayStation for testing
- **Violates Single Responsibility:** Gamer shouldn't know how to create PlayStation

### Step 2: Using @Bean (Spring Configuration)

**@Bean Definition:** A method that tells Spring how to create and configure an object. Spring will manage this object's lifecycle.

```java
// PlayStation class
public class PlayStation {
    public void startGame() {
        System.out.println("PlayStation started!");
    }
}

// Gamer class - receives PlayStation from outside
public class Gamer {
    private PlayStation playStation;
    
    public Gamer(PlayStation playStation) {
        this.playStation = playStation;
    }
    
    public void playGame() {
        playStation.startGame();
        System.out.println("Gamer is playing!");
    }
}

// Spring Configuration - Manual bean creation
@Configuration
public class GamingConfig {
    
    @Bean
    public PlayStation playStation() {
        return new PlayStation(); // Spring manages this object
    }
    
    @Bean
    public Gamer gamer(PlayStation playStation) {
        return new Gamer(playStation); // Spring injects PlayStation
    }
}

// Main class
@SpringBootApplication
public class BeanApp {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(BeanApp.class, args);
        
        Gamer gamer = context.getBean(Gamer.class);
        gamer.playGame();
    }
}
```

**What's happening here:**
- `@Configuration` tells Spring this class contains bean definitions
- `@Bean` methods tell Spring how to create objects
- Spring creates PlayStation first, then Gamer, then injects PlayStation into Gamer
- **Loose coupling:** Gamer doesn't create PlayStation, Spring does

### Step 3: Using @Autowired (Automatic Injection)

**@Autowired Definition:** An annotation that tells Spring to automatically inject dependencies. Spring will find the right object and inject it.

```java
// PlayStation as Spring component
@Component
public class PlayStation {
    public void startGame() {
        System.out.println("PlayStation started!");
    }
}

// Gamer with @Autowired
@Component
public class Gamer {
    private final PlayStation playStation;
    
    // Spring automatically injects PlayStation
    public Gamer(PlayStation playStation) {
        this.playStation = playStation;
    }
    
    public void playGame() {
        playStation.startGame();
        System.out.println("Gamer is playing!");
    }
}

// Main class - No configuration needed!
@SpringBootApplication
public class AutowiredApp {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(AutowiredApp.class, args);
        
        Gamer gamer = context.getBean(Gamer.class);
        gamer.playGame();
    }
}
```

**What's happening here:**
- `@Component` tells Spring to automatically create and manage this object
- `@Autowired` (optional for constructor injection) tells Spring to inject dependencies
- Spring automatically finds PlayStation and injects it into Gamer
- **No manual configuration needed!**

## What is a Bean?

**Bean Definition:** An object that is created, configured, and managed by the Spring container. Beans are the backbone of a Spring application.

```java
// Manual bean creation
@Bean
public PlayStation playStation() {
    return new PlayStation(); // Spring manages this object
}

// Automatic bean creation
@Component
public class PlayStation {
    // Spring automatically creates and manages this object
}
```

**Bean Lifecycle:**
1. **Creation:** Spring creates the object using constructor or factory method
2. **Configuration:** Spring injects dependencies and sets properties
3. **Initialization:** Spring calls initialization methods (@PostConstruct)
4. **Management:** Spring keeps the object alive and manages it
5. **Destruction:** Spring calls cleanup methods (@PreDestroy) and destroys the object

**Bean vs Regular Object:**
- **Regular Object:** You create it with `new`, you manage its lifecycle
- **Bean:** Spring creates it, Spring manages its lifecycle

## What is IoC (Inversion of Control)?

**IoC Definition:** A design principle where the control of object creation and dependency management is inverted from the application code to an external framework (Spring container).

**Before IoC:**
```java
public class Gamer {
    public Gamer() {
        this.playStation = new PlayStation(); // Gamer controls PlayStation creation
    }
}
```

**After IoC:**
```java
public class Gamer {
    public Gamer(PlayStation playStation) {
        this.playStation = playStation; // Spring controls PlayStation creation
    }
}
```

**IoC Benefits:**
- **Loose coupling:** Objects don't create their own dependencies
- **Testability:** Easy to inject mock objects for testing
- **Flexibility:** Can switch implementations without changing code
- **Single Responsibility:** Objects focus on their core logic, not dependency creation

## Types of DI (Dependency Injection)

**DI Definition:** A design pattern where dependencies are provided to an object rather than the object creating them itself.

### 1. Constructor Injection (Best Practice)
```java
@Component
public class Gamer {
    private final PlayStation playStation;
    
    // Constructor injection - Spring injects PlayStation here
    public Gamer(PlayStation playStation) {
        this.playStation = playStation;
    }
}
```

**Why it's best:**
- **Immutable:** Fields can be final
- **Null safety:** Dependencies are guaranteed to be available
- **Testability:** Easy to test with mock objects
- **Thread safety:** No setter methods to worry about

### 2. Setter Injection
```java
@Component
public class Gamer {
    private PlayStation playStation;
    
    @Autowired
    public void setPlayStation(PlayStation playStation) {
        this.playStation = playStation;
    }
}
```

**When to use:**
- Optional dependencies
- When you need to change dependencies at runtime

### 3. Field Injection (Not Recommended)
```java
@Component
public class Gamer {
    @Autowired
    private PlayStation playStation;
}
```

**Why it's not recommended:**
- **Hard to test:** Can't easily inject mocks
- **Not immutable:** Fields can be null
- **Hidden dependencies:** Not obvious what dependencies exist

## Different Types of @Autowired Injection

### 1. Constructor Injection with @Autowired (Explicit)
```java
@Component
public class Gamer {
    private final PlayStation playStation;
    
    @Autowired
    public Gamer(PlayStation playStation) {
        this.playStation = playStation;
    }
}
```

### 2. Setter Injection with @Autowired
```java
@Component
public class Gamer {
    private PlayStation playStation;
    
    @Autowired
    public void setPlayStation(PlayStation playStation) {
        this.playStation = playStation;
    }
}
```

### 3. Field Injection with @Autowired
```java
@Component
public class Gamer {
    @Autowired
    private PlayStation playStation;
}
```

### 4. Multiple Dependencies with @Autowired
```java
@Component
public class ProGamer {
    private final PlayStation playStation;
    private final Xbox xbox;
    private final NintendoSwitch nintendoSwitch;
    
    @Autowired
    public ProGamer(PlayStation playStation, Xbox xbox, NintendoSwitch nintendoSwitch) {
        this.playStation = playStation;
        this.xbox = xbox;
        this.nintendoSwitch = nintendoSwitch;
    }
}
```

### 7. @Autowired with @Qualifier
```java
@Component
public class Gamer {
    @Autowired
    @Qualifier("playStation")
    private GamingConsole primaryConsole;
    
    @Autowired
    @Qualifier("xbox")
    private GamingConsole secondaryConsole;
}

Absolutely, Jason 🎮 — here’s the **complete working Spring Boot example** showing how `@Autowired` with `@Qualifier` works, using the **Gamer–PlayStation–Xbox** analogy.

---

## ⚙️ Complete Example: `@Autowired` + `@Qualifier`

---

### `GamingConsole.java`

```java
package com.example.gaming;

public interface GamingConsole {
    void play();
}
```

---

### `PlayStation.java`

```java
package com.example.gaming;

import org.springframework.stereotype.Component;

@Component("playStation") // Custom bean name (optional)
public class PlayStation implements GamingConsole {

    @Override
    public void play() {
        System.out.println("🎮 Playing on PlayStation 5");
    }
}
```

---

### `Xbox.java`

```java
package com.example.gaming;

import org.springframework.stereotype.Component;

@Component("xbox") // Custom bean name (optional)
public class Xbox implements GamingConsole {

    @Override
    public void play() {
        System.out.println("🕹️ Playing on Xbox Series X");
    }
}
```

---

### `Gamer.java`

```java
package com.example.gaming;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class Gamer {

    private final GamingConsole console;

    // ✅ Constructor-based injection (modern, recommended)
    @Autowired
    public Gamer(@Qualifier("playStation") GamingConsole console) {
        this.console = console;
    }

    public void startGaming() {
        console.play();
    }
}
```

---

### `GamingApp.java`

```java
package com.example.gaming;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootApplication
public class GamingApp implements CommandLineRunner {

    @Autowired
    private Gamer gamer;

    public static void main(String[] args) {
        SpringApplication.run(GamingApp.class, args);
    }

    @Override
    public void run(String... args) {
        gamer.startGaming();
    }
}
```

---

## 🧩 Output

```
🎮 Playing on PlayStation 5
```

---

### 💡 To switch console:

Just change the qualifier in `Gamer.java`:

```java
@Qualifier("xbox")
```

Then the output becomes:

```
🕹️ Playing on Xbox Series X
```

---

✅ **Concept Recap:**

* `@Autowired` → Injects dependency automatically.
* `@Qualifier("beanName")` → Tells Spring **which bean** to inject when multiple beans of the same type exist.
* **Constructor injection** → Recommended for cleaner, immutable dependencies.

---

Would you like me to extend this example to show how **@Primary** can remove the need for `@Qualifier` (like setting a default console)?

```

### 8. @Autowired with @Primary
```java
@Component
@Primary
public class PlayStation implements GamingConsole {
    // This will be the default console when multiple implementations exist
}

@Component
public class Gamer {
    @Autowired
    private GamingConsole console; // Will inject PlayStation because it's @Primary
}
```

### 9. @Autowired with @Lazy
```java
@Component
public class Gamer {
    private final GamingConsole console;
    
    @Autowired
    public Gamer(@Lazy GamingConsole console) {
        this.console = console; // Console will be created only when first used
    }
}
```

### 10. @Autowired with @Value
```java
@Component
public class Gamer {
    @Autowired
    private GamingConsole console;
    
    @Value("${gaming.player.name:DefaultPlayer}")
    private String playerName;
    
    @Value("${gaming.player.level:1}")
    private int playerLevel;
}
```

### 11. @Autowired with @Resource (JSR-250)
```java
@Component
public class Gamer {
    @Resource(name = "playStation")
    private GamingConsole console;
    
    @Resource
    private GamingConsole defaultConsole; // Uses field name as bean name
}
```

### 12. @Autowired with @Inject (JSR-330)
```java
@Component
public class Gamer {
    @Inject
    private GamingConsole console;
    
    @Inject
    @Named("playStation")
    private GamingConsole specificConsole;
}
```

### 13. @Autowired with Custom Annotations
```java
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Autowired
@Qualifier("playStation")
public @interface PlayStationConsole {
}

@Component
public class Gamer {
    @PlayStationConsole
    private GamingConsole console;
}
```

### 14. @Autowired with Method Parameters
```java
@Component
public class Gamer {
    private GamingConsole console;
    
    @Autowired
    public void configureConsole(GamingConsole console) {
        this.console = console;
    }
}
```

### 15. @Autowired with Constructor Parameters
```java
@Component
public class Gamer {
    private final GamingConsole console;
    private final String playerName;
    
    @Autowired
    public Gamer(GamingConsole console, @Value("${gaming.player.name}") String playerName) {
        this.console = console;
        this.playerName = playerName;
    }
}
```

## @Autowired Best Practices

### 1. Use Constructor Injection (Recommended)
```java
@Component
public class Gamer {
    private final PlayStation playStation;
    
    // @Autowired is optional for constructor injection in newer Spring versions
    public Gamer(PlayStation playStation) {
        this.playStation = playStation;
    }
}
```

### 2. Use @Autowired(required = false) for Optional Dependencies
```java
@Component
public class Gamer {
    @Autowired(required = false)
    private GamingConsole optionalConsole;
    
    public void playGame() {
        if (optionalConsole != null) {
            optionalConsole.startGame();
        }
    }
}
```

### 3. Use @Qualifier for Multiple Implementations
```java
@Component
public class Gamer {
    @Autowired
    @Qualifier("playStation")
    private GamingConsole primaryConsole;
    
    @Autowired
    @Qualifier("xbox")
    private GamingConsole secondaryConsole;
}
```

### 4. Use @Primary for Default Implementation
```java
@Component
@Primary
public class PlayStation implements GamingConsole {
    // This will be the default when multiple implementations exist
}
```

### 5. Use @Lazy for Expensive Dependencies
```java
@Component
public class Gamer {
    @Autowired
    @Lazy
    private GamingConsole expensiveConsole; // Created only when first used
}
```

---

## 2. Spring Beans & Configuration

### Spring Annotations Explained

#### 1. @Configuration

**@Configuration Definition:** Tells Spring that this class contains bean definitions and configuration methods.

```java
@Configuration
public class GamingConfig {
    
    @Bean
    public PlayStation playStation() {
        return new PlayStation();
    }
    
    @Bean
    public Gamer gamer(PlayStation playStation) {
        return new Gamer(playStation);
    }
}
```

**What it does:**
- Marks a class as a configuration class
- Spring scans this class for @Bean methods
- Used for manual bean creation
- Good for configuring third-party libraries

**Simple Analogy:** Like a recipe book that tells Spring how to create objects.

#### 2. @Component

**@Component Definition:** Tells Spring to automatically create and manage this object as a bean.

```java
@Component
public class PlayStation {
    public void startGame() {
        System.out.println("🎮 PlayStation started!");
    }
}

@Component
public class Gamer {
    private final PlayStation playStation;
    
    public Gamer(PlayStation playStation) {
        this.playStation = playStation;
    }
    
    public void playGame() {
        playStation.startGame();
        System.out.println("👾 Gamer is playing!");
    }
}
```

**What it does:**
- Spring automatically creates this object
- Spring manages its lifecycle
- Spring injects dependencies
- No manual configuration needed

**Simple Analogy:** Like telling Spring "I need this object, create it for me."

#### 3. @ComponentScan

**@ComponentScan Definition:** Tells Spring which packages to scan for @Component classes.

```java
@SpringBootApplication
@ComponentScan(basePackages = "com.example.gaming")
public class GamingApp {
    public static void main(String[] args) {
        SpringApplication.run(GamingApp.class, args);
    }
}
```

**What it does:**
- Spring scans specified packages for @Component classes
- Automatically creates beans from found classes
- Can scan multiple packages
- Can exclude certain classes

**Simple Analogy:** Like telling Spring "Look in these folders for objects I need."

#### 4. @Bean

**@Bean Definition:** A method that tells Spring how to create and configure an object manually.

```java
@Configuration
public class GamingConfig {
    
    @Bean
    public PlayStation playStation() {
        PlayStation ps = new PlayStation();
        // You can configure the PlayStation here
        ps.setModel("PS5");
        return ps;
    }
    
    @Bean
    public Gamer gamer(PlayStation playStation) {
        return new Gamer(playStation);
    }
}
```

**What it does:**
- Manual bean creation
- Full control over object creation
- Can configure objects before returning
- Good for third-party libraries

**Simple Analogy:** Like a factory method that creates objects step by step.

#### 5. @Service

**@Service Definition:** A special type of @Component used for business logic classes.

```java
@Service
public class GamingService {
    
    public void startGamingSession() {
        System.out.println("🎮 Gaming session started!");
    }
    
    public void endGamingSession() {
        System.out.println("🎮 Gaming session ended!");
    }
    
    public void playGame(String gameName) {
        System.out.println("🎮 Playing " + gameName);
    }
}

@Component
public class Gamer {
    private final GamingService gamingService;
    
    public Gamer(GamingService gamingService) {
        this.gamingService = gamingService;
    }
    
    public void playGame() {
        gamingService.startGamingSession();
        gamingService.playGame("FIFA");
        gamingService.endGamingSession();
    }
}
```

**What it does:**
- Indicates business logic layer
- Same as @Component but more specific
- Spring creates and manages it
- Used for service classes

**Simple Analogy:** Like a service desk that handles business operations.

#### 6. @Repository

**@Repository Definition:** A special type of @Component used for data access classes.

```java
@Repository
public class GameRepository {
    
    public List<String> getAvailableGames() {
        return Arrays.asList("FIFA", "Call of Duty", "Minecraft");
    }
    
    public void saveGameProgress(String gameName, int level) {
        System.out.println("💾 Saved progress for " + gameName + " at level " + level);
    }
    
    public void loadGameProgress(String gameName) {
        System.out.println("💾 Loaded progress for " + gameName);
    }
}

@Service
public class GamingService {
    private final GameRepository gameRepository;
    
    public GamingService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }
    
    public void playGame(String gameName) {
        List<String> games = gameRepository.getAvailableGames();
        if (games.contains(gameName)) {
            System.out.println("🎮 Playing " + gameName);
            gameRepository.saveGameProgress(gameName, 1);
        }
    }
}
```

**What it does:**
- Indicates data access layer
- Same as @Component but more specific
- Spring creates and manages it
- Used for database operations

**Simple Analogy:** Like a storage room that handles data operations.

#### 7. @Controller

**@Controller Definition:** A special type of @Component used for web layer classes.

```java
@Controller
public class GamingController {
    private final GamingService gamingService;
    
    public GamingController(GamingService gamingService) {
        this.gamingService = gamingService;
    }
    
    @RequestMapping("/play")
    public String playGame(@RequestParam String gameName) {
        gamingService.playGame(gameName);
        return "Game started: " + gameName;
    }
    
    @RequestMapping("/games")
    public String getGames() {
        return "Available games: FIFA, Call of Duty, Minecraft";
    }
}
```

**What it does:**
- Indicates web layer
- Same as @Component but more specific
- Spring creates and manages it
- Used for web endpoints

**Simple Analogy:** Like a reception desk that handles web requests.

#### 8. @Profile

**@Profile Definition:** Specifies which profile(s) a bean belongs to. Beans are only created when their profile is active.

```java
@Component
@Profile("playstation")
public class PlayStation {
    public void startGame() {
        System.out.println("🎮 PlayStation started game!");
    }
}

@Component
@Profile("xbox")
public class Xbox {
    public void startGame() {
        System.out.println("🎮 Xbox started game!");
    }
}

@Component
@Profile("nintendo")
public class NintendoSwitch {
    public void startGame() {
        System.out.println("🎮 Nintendo Switch started game!");
    }
}

@Component
public class Gamer {
    private final GamingConsole console;
    
    public Gamer(GamingConsole console) {
        this.console = console;
    }
    
    public void playGame() {
        console.startGame();
        System.out.println("👾 Gamer is playing!");
    }
}
```

**What it does:**
- Creates beans only when specific profile is active
- Allows different configurations for different environments
- Useful for dev, test, prod environments

**Simple Analogy:** Like having different PlayStation models for different rooms.

#### 9. @Conditional

**@Conditional Definition:** Creates beans only when specific conditions are met.

```java
@Component
@Conditional(PlayStationCondition.class)
public class PlayStation {
    public void startGame() {
        System.out.println("🎮 PlayStation started game!");
    }
}

public class PlayStationCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // Check if PlayStation is available
        return System.getProperty("gaming.console").equals("playstation");
    }
}
```

**What it does:**
- Creates beans based on custom conditions
- Full control over when beans are created
- Useful for complex conditional logic

**Simple Analogy:** Like checking if you have the right controller before creating a game.

#### 10. @ConditionalOnProperty

**@ConditionalOnProperty Definition:** Creates beans based on property values.

```java
@Component
@ConditionalOnProperty(name = "gaming.console", havingValue = "playstation")
public class PlayStation {
    public void startGame() {
        System.out.println("🎮 PlayStation started game!");
    }
}

@Component
@ConditionalOnProperty(name = "gaming.console", havingValue = "xbox")
public class Xbox {
    public void startGame() {
        System.out.println("🎮 Xbox started game!");
    }
}

@Component
@ConditionalOnProperty(name = "gaming.multiplayer", havingValue = "true")
public class MultiplayerService {
    public void startMultiplayer() {
        System.out.println("👥 Starting multiplayer game!");
    }
}
```

**What it does:**
- Creates beans based on application properties
- Simple property-based conditional logic
- Useful for feature toggles

**Simple Analogy:** Like turning on features based on settings.

#### 11. @ConditionalOnClass

**@ConditionalOnClass Definition:** Creates beans only when specific classes are present on the classpath.

```java
@Component
@ConditionalOnClass(name = "com.example.PlayStation")
public class PlayStationService {
    public void startPlayStation() {
        System.out.println("🎮 PlayStation service started!");
    }
}

@Component
@ConditionalOnClass(name = "com.example.Xbox")
public class XboxService {
    public void startXbox() {
        System.out.println("🎮 Xbox service started!");
    }
}

@Component
@ConditionalOnClass(name = "com.example.NintendoSwitch")
public class NintendoService {
    public void startNintendo() {
        System.out.println("🎮 Nintendo service started!");
    }
}
```

**What it does:**
- Creates beans only when specific classes are available
- Useful for optional dependencies
- Prevents errors when classes are missing

**Simple Analogy:** Like checking if you have the right game disc before starting.

### Complete Working Example

```java
// PlayStation.java
@Component
public class PlayStation {
    public void startGame() {
        System.out.println("🎮 PlayStation started!");
    }
}

// GamingService.java
@Service
public class GamingService {
    public void startGamingSession() {
        System.out.println("🎮 Gaming session started!");
    }
}

// GameRepository.java
@Repository
public class GameRepository {
    public List<String> getGames() {
        return Arrays.asList("FIFA", "Call of Duty");
    }
}

// GamingController.java
@Controller
public class GamingController {
    private final GamingService gamingService;
    
    public GamingController(GamingService gamingService) {
        this.gamingService = gamingService;
    }
    
    @RequestMapping("/play")
    public String playGame() {
        gamingService.startGamingSession();
        return "Game started!";
    }
}

// Gamer.java
@Component
public class Gamer {
    private final PlayStation playStation;
    private final GamingService gamingService;
    private final GameRepository gameRepository;
    
    public Gamer(PlayStation playStation, GamingService gamingService, GameRepository gameRepository) {
        this.playStation = playStation;
        this.gamingService = gamingService;
        this.gameRepository = gameRepository;
    }
    
    public void playGame() {
        gamingService.startGamingSession();
        playStation.startGame();
        System.out.println("👾 Gamer is playing!");
    }
}

// GamingApp.java
@SpringBootApplication
@ComponentScan(basePackages = "com.example.gaming")
public class GamingApp {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(GamingApp.class, args);
        
        Gamer gamer = context.getBean(Gamer.class);
        gamer.playGame();
    }
}
```

### Key Differences:

| Annotation | Purpose | Layer | When to Use |
|------------|---------|-------|-------------|
| `@Component` | General purpose | Any | Your own classes |
| `@Service` | Business logic | Service | Business operations |
| `@Repository` | Data access | Data | Database operations |
| `@Controller` | Web layer | Web | Web endpoints |
| `@Configuration` | Configuration | Config | Bean definitions |
| `@Bean` | Manual creation | Config | Third-party libraries |
| `@ComponentScan` | Package scanning | Config | Tell Spring where to look |
| `@Profile` | Environment-specific | Any | Different environments |
| `@Conditional` | Custom conditions | Any | Complex conditional logic |
| `@ConditionalOnProperty` | Property-based | Any | Feature toggles |
| `@ConditionalOnClass` | Class-based | Any | Optional dependencies |

### Simple Memory Trick:

- **@Component** = "I need this object"
- **@Service** = "I need this business service"
- **@Repository** = "I need this data storage"
- **@Controller** = "I need this web handler"
- **@Configuration** = "I need to configure objects"
- **@Bean** = "I need to create this object manually"
- **@ComponentScan** = "I need Spring to find my objects"
- **@Profile** = "I need this only in specific environment"
- **@Conditional** = "I need this only when custom condition is met"
- **@ConditionalOnProperty** = "I need this only when property is set"
- **@ConditionalOnClass** = "I need this only when class is available"

### Java-based vs XML vs Annotation-based Configuration

#### Java-based Configuration (Recommended)

**Java-based Definition:** Configuration using Java classes with annotations.

```java
@Configuration
public class GamingConfig {
    
    @Bean
    public PlayStation playStation() {
        PlayStation ps = new PlayStation();
        ps.setModel("PS5");
        return ps;
    }
    
    @Bean
    public Gamer gamer(PlayStation playStation) {
        return new Gamer(playStation);
    }
}

// Main class
@SpringBootApplication
public class GamingApp {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(GamingApp.class, args);
        
        Gamer gamer = context.getBean(Gamer.class);
        gamer.playGame();
    }
}
```

**Advantages:**
- **Type-safe:** Compile-time checking
- **IDE support:** Auto-completion and refactoring
- **Easy to debug:** Can set breakpoints
- **Flexible:** Can use any Java code
- **Modern:** Recommended approach

#### XML-based Configuration

**XML-based Definition:** Configuration using XML files.

```xml
<!-- gaming-config.xml -->
<beans xmlns="http://www.springframework.org/schema/beans">
    <bean id="playStation" class="com.example.PlayStation">
        <property name="model" value="PS5"/>
    </bean>
    
    <bean id="gamer" class="com.example.Gamer">
        <constructor-arg ref="playStation"/>
    </bean>
</beans>
```

```java
// Main class
public class GamingApp {
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("gaming-config.xml");
        
        Gamer gamer = context.getBean(Gamer.class);
        gamer.playGame();
    }
}
```

**Advantages:**
- **External configuration:** Can change without recompiling
- **Separation of concerns:** Configuration separate from code
- **Legacy support:** Works with older Spring versions

**Disadvantages:**
- **No type safety:** Runtime errors only
- **Verbose:** Lots of XML to write
- **No IDE support:** Limited auto-completion

#### Annotation-based Configuration

**Annotation-based Definition:** Configuration using annotations on classes.

```java
@Component
public class PlayStation {
    public void startGame() {
        System.out.println("🎮 PlayStation started!");
    }
}

@Component
public class Gamer {
    private final PlayStation playStation;
    
    public Gamer(PlayStation playStation) {
        this.playStation = playStation;
    }
    
    public void playGame() {
        playStation.startGame();
        System.out.println("👾 Gamer is playing!");
    }
}

// Main class
@SpringBootApplication
@ComponentScan(basePackages = "com.example.gaming")
public class GamingApp {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(GamingApp.class, args);
        
        Gamer gamer = context.getBean(Gamer.class);
        gamer.playGame();
    }
}
```

**Advantages:**
- **Simple:** Just add annotations
- **Automatic:** Spring handles everything
- **Less code:** No manual configuration
- **Modern:** Recommended for new projects

**Disadvantages:**
- **Less control:** Can't configure complex objects easily
- **Hidden dependencies:** Not obvious what's being injected

---

## 3. Spring Bean Lifecycle

### Spring Bean Lifecycle - Simple Explanation

#### What is Bean Lifecycle?

**Think of it like this:** When you buy a PlayStation, it goes through different stages:
1. **Manufacturing** (Constructor)
2. **Setup** (Initialization) 
3. **Ready to Use** (Bean is available)
4. **Playing Games** (Using the bean)
5. **Shutting Down** (Cleanup before disposal)

#### The 5 Stages of Bean Lifecycle

##### Stage 1: Constructor (Object Creation)
```java
@Component
public class PlayStation {
    public PlayStation() {
        System.out.println("🏭 PlayStation is being made!");
    }
}
```

##### Stage 2: Dependency Injection
```java
@Component
public class Gamer {
    private final PlayStation playStation;
    
    public Gamer(PlayStation playStation) {
        System.out.println("👤 Gamer is created with PlayStation!");
        this.playStation = playStation;
    }
}
```

##### Stage 3: Initialization (@PostConstruct)
```java
@Component
public class PlayStation {
    private boolean isReady;
    
    public PlayStation() {
        System.out.println("🏭 PlayStation is being made!");
    }
    
    @PostConstruct
    public void setup() {
        System.out.println("🔧 PlayStation is being set up!");
        this.isReady = true;
        System.out.println("✅ PlayStation is ready to use!");
    }
}
```

##### Stage 4: Ready to Use
```java
@Component
public class Gamer {
    private final PlayStation playStation;
    
    public Gamer(PlayStation playStation) {
        this.playStation = playStation;
    }
    
    @PostConstruct
    public void setup() {
        System.out.println("👾 Gamer is ready to play!");
    }
    
    public void playGame() {
        playStation.startGame();
        System.out.println("👾 Gamer is playing!");
    }
}
```

##### Stage 5: Cleanup (@PreDestroy)
```java
@Component
public class PlayStation {
    private boolean isReady;
    
    @PreDestroy
    public void cleanup() {
        System.out.println("🧹 Cleaning up PlayStation...");
        System.out.println("💾 Saving game progress...");
        System.out.println("🔌 Turning off PlayStation...");
        System.out.println("✅ PlayStation cleanup done!");
    }
}
```

### Complete Simple Example

```java
// PlayStation.java
@Component
public class PlayStation {
    private boolean isReady;
    
    public PlayStation() {
        System.out.println("🏭 PlayStation is being made!");
    }
    
    @PostConstruct
    public void setup() {
        System.out.println("🔧 PlayStation is being set up!");
        this.isReady = true;
        System.out.println("✅ PlayStation is ready to use!");
    }
    
    public void startGame() {
        if (isReady) {
            System.out.println("🎮 PlayStation started game!");
        } else {
            System.out.println("❌ PlayStation not ready yet!");
        }
    }
    
    @PreDestroy
    public void cleanup() {
        System.out.println("🧹 Cleaning up PlayStation...");
        System.out.println("💾 Saving game progress...");
        System.out.println("🔌 Turning off PlayStation...");
        System.out.println("✅ PlayStation cleanup done!");
    }
}

// Gamer.java
@Component
public class Gamer {
    private final PlayStation playStation;
    
    public Gamer(PlayStation playStation) {
        System.out.println("👤 Gamer is created with PlayStation!");
        this.playStation = playStation;
    }
    
    @PostConstruct
    public void setup() {
        System.out.println("👾 Gamer is ready to play!");
    }
    
    public void playGame() {
        playStation.startGame();
        System.out.println("👾 Gamer is playing!");
    }
    
    @PreDestroy
    public void cleanup() {
        System.out.println("🧹 Cleaning up Gamer...");
        System.out.println("💾 Saving player progress...");
        System.out.println("👋 Gamer signing off!");
    }
}

// GamingApp.java
@SpringBootApplication
public class GamingApp {
    public static void main(String[] args) {
        System.out.println("=== Starting Gaming App ===");
        
        ApplicationContext context = SpringApplication.run(GamingApp.class, args);
        
        System.out.println("\n=== App Started ===");
        Gamer gamer = context.getBean(Gamer.class);
        gamer.playGame();
        
        System.out.println("\n=== Shutting Down ===");
        ((ConfigurableApplicationContext) context).close();
    }
}
```

### What You'll See When Running:

```
=== Starting Gaming App ===
🏭 PlayStation is being made!
👤 Gamer is created with PlayStation!
🔧 PlayStation is being set up!
✅ PlayStation is ready to use!
👾 Gamer is ready to play!

=== App Started ===
🎮 PlayStation started game!
👾 Gamer is playing!

=== Shutting Down ===
🧹 Cleaning up Gamer...
💾 Saving player progress...
👋 Gamer signing off!
🧹 Cleaning up PlayStation...
💾 Saving game progress...
🔌 Turning off PlayStation...
✅ PlayStation cleanup done!
```

### Simple Rules to Remember:

1. **@PostConstruct** = "After everything is set up, do this"
2. **@PreDestroy** = "Before shutting down, do this"
3. **Constructor** = "When creating the object"
4. **Dependency Injection** = "When Spring gives you what you need"

### Real-World Analogy:

**Buying a PlayStation:**
1. **Constructor** = You buy the PlayStation from the store
2. **Dependency Injection** = You get the controller and games
3. **@PostConstruct** = You set it up at home, connect to TV
4. **Ready to Use** = You can now play games
5. **@PreDestroy** = Before moving, you pack it up safely

### Key Points:

- **@PostConstruct** runs AFTER dependencies are injected
- **@PreDestroy** runs BEFORE the bean is destroyed
- Spring manages everything automatically
- You just need to add the annotations
- Use @PostConstruct for setup
- Use @PreDestroy for cleanup

---

## 4. Bean Scopes

### What are Bean Scopes?

**Bean Scope Definition:** Determines how many instances of a bean Spring creates and how long they live.

**Simple Analogy:** Think of it like different types of PlayStation ownership:
- **Singleton** = One PlayStation shared by everyone in the house
- **Prototype** = Each person gets their own PlayStation
- **Request** = New PlayStation for each web request
- **Session** = One PlayStation per user session

### 1. Singleton Scope (Default)

**Singleton Definition:** Only one instance of the bean exists in the entire application.

```java
@Component
@Scope("singleton") // This is the default, so @Scope is optional
public class PlayStation {
    private String model;
    private int gameCount;
    
    public PlayStation() {
        this.model = "PS5";
        this.gameCount = 0;
        System.out.println("🏭 PlayStation created! Model: " + model);
    }
    
    public void startGame() {
        gameCount++;
        System.out.println("🎮 PlayStation started game #" + gameCount);
    }
    
    public int getGameCount() {
        return gameCount;
    }
}

@Component
public class Gamer {
    private final PlayStation playStation;
    
    public Gamer(PlayStation playStation) {
        this.playStation = playStation;
    }
    
    public void playGame() {
        playStation.startGame();
        System.out.println("👾 Gamer is playing!");
    }
}

@SpringBootApplication
public class GamingApp {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(GamingApp.class, args);
        
        // Get the same PlayStation instance
        PlayStation ps1 = context.getBean(PlayStation.class);
        PlayStation ps2 = context.getBean(PlayStation.class);
        
        System.out.println("Same PlayStation? " + (ps1 == ps2)); // true
        
        ps1.startGame(); // Game #1
        ps2.startGame(); // Game #2 (same instance!)
        
        System.out.println("Total games: " + ps1.getGameCount()); // 2
    }
}
```

**Output:**
```
🏭 PlayStation created! Model: PS5
Same PlayStation? true
🎮 PlayStation started game #1
🎮 PlayStation started game #2
Total games: 2
```

### 2. Prototype Scope

**Prototype Definition:** A new instance is created every time the bean is requested.

```java
@Component
@Scope("prototype")
public class Game {
    private String gameName;
    private int playCount;
    
    public Game() {
        this.playCount = 0;
        System.out.println("🎮 New game created!");
    }
    
    public void setGameName(String gameName) {
        this.gameName = gameName;
    }
    
    public void play() {
        playCount++;
        System.out.println("🎮 Playing " + gameName + " #" + playCount);
    }
}

@Component
public class Gamer {
    private final Game game;
    
    public Gamer(Game game) {
        this.game = game;
        game.setGameName("FIFA");
    }
    
    public void playGame() {
        game.play();
        System.out.println("👾 Gamer is playing!");
    }
}

@SpringBootApplication
public class GamingApp {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(GamingApp.class, args);
        
        // Get different Game instances
        Game game1 = context.getBean(Game.class);
        Game game2 = context.getBean(Game.class);
        
        System.out.println("Same game? " + (game1 == game2)); // false
        
        game1.setGameName("FIFA");
        game2.setGameName("Call of Duty");
        
        game1.play(); // FIFA #1
        game2.play(); // Call of Duty #1
    }
}
```

**Output:**
```
🎮 New game created!
🎮 New game created!
Same game? false
🎮 Playing FIFA #1
🎮 Playing Call of Duty #1
```

### 3. Request Scope (Web Applications)

**Request Scope Definition:** A new instance is created for each HTTP request.

```java
@Component
@Scope("request")
public class GameSession {
    private String sessionId;
    private List<String> gamesPlayed;
    
    public GameSession() {
        this.sessionId = UUID.randomUUID().toString();
        this.gamesPlayed = new ArrayList<>();
        System.out.println("🌐 New game session created: " + sessionId);
    }
    
    public void addGame(String gameName) {
        gamesPlayed.add(gameName);
        System.out.println("🎮 Added game: " + gameName);
    }
    
    public List<String> getGamesPlayed() {
        return gamesPlayed;
    }
}

@Controller
public class GamingController {
    private final GameSession gameSession;
    
    public GamingController(GameSession gameSession) {
        this.gameSession = gameSession;
    }
    
    @RequestMapping("/play")
    public String playGame(@RequestParam String gameName) {
        gameSession.addGame(gameName);
        return "Playing: " + gameName;
    }
}
```

### 4. Session Scope (Web Applications)

**Session Scope Definition:** One instance per HTTP session (user).

```java
@Component
@Scope("session")
public class PlayerProfile {
    private String playerName;
    private int level;
    private List<String> achievements;
    
    public PlayerProfile() {
        this.level = 1;
        this.achievements = new ArrayList<>();
        System.out.println("👤 New player profile created!");
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public void levelUp() {
        level++;
        System.out.println("🎉 " + playerName + " leveled up to " + level);
    }
    
    public void addAchievement(String achievement) {
        achievements.add(achievement);
        System.out.println("🏆 Achievement unlocked: " + achievement);
    }
}

@Controller
public class PlayerController {
    private final PlayerProfile playerProfile;
    
    public PlayerController(PlayerProfile playerProfile) {
        this.playerProfile = playerProfile;
    }
    
    @RequestMapping("/login")
    public String login(@RequestParam String playerName) {
        playerProfile.setPlayerName(playerName);
        return "Welcome " + playerName;
    }
    
    @RequestMapping("/levelup")
    public String levelUp() {
        playerProfile.levelUp();
        return "Level up!";
    }
}
```

### 5. Application Scope (Web Applications)

**Application Scope Definition:** One instance per web application context.

```java
@Component
@Scope("application")
public class GameStatistics {
    private int totalGamesPlayed;
    private int totalPlayers;
    private Map<String, Integer> gamePopularity;
    
    public GameStatistics() {
        this.totalGamesPlayed = 0;
        this.totalPlayers = 0;
        this.gamePopularity = new HashMap<>();
        System.out.println("📊 Game statistics initialized!");
    }
    
    public void recordGamePlayed(String gameName) {
        totalGamesPlayed++;
        gamePopularity.merge(gameName, 1, Integer::sum);
        System.out.println("📈 Game played: " + gameName);
    }
    
    public void recordNewPlayer() {
        totalPlayers++;
        System.out.println("👤 New player registered!");
    }
    
    public Map<String, Integer> getGamePopularity() {
        return gamePopularity;
    }
}
```

### 6. WebSocket Scope (WebSocket Applications)

**WebSocket Scope Definition:** One instance per WebSocket session.

```java
@Component
@Scope("websocket")
public class WebSocketGameSession {
    private String sessionId;
    private String playerName;
    private boolean isConnected;
    
    public WebSocketGameSession() {
        this.sessionId = UUID.randomUUID().toString();
        this.isConnected = true;
        System.out.println("🔌 WebSocket game session created: " + sessionId);
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public void sendMessage(String message) {
        System.out.println("💬 Sending to " + playerName + ": " + message);
    }
    
    public void disconnect() {
        this.isConnected = false;
        System.out.println("🔌 WebSocket disconnected: " + sessionId);
    }
}
```

### Complete Scope Comparison Example

```java
@Component
@Scope("singleton")
public class PlayStation {
    private int gameCount;
    
    public PlayStation() {
        System.out.println("🏭 PlayStation created (Singleton)");
    }
    
    public void startGame() {
        gameCount++;
        System.out.println("🎮 PlayStation game #" + gameCount);
    }
}

@Component
@Scope("prototype")
public class Game {
    private String gameName;
    
    public Game() {
        System.out.println("🎮 Game created (Prototype)");
    }
    
    public void setGameName(String gameName) {
        this.gameName = gameName;
    }
    
    public void play() {
        System.out.println("🎮 Playing " + gameName);
    }
}

@Component
public class Gamer {
    private final PlayStation playStation;
    private final Game game;
    
    public Gamer(PlayStation playStation, Game game) {
        this.playStation = playStation;
        this.game = game;
        game.setGameName("FIFA");
    }
    
    public void playGame() {
        playStation.startGame();
        game.play();
        System.out.println("👾 Gamer is playing!");
    }
}

@SpringBootApplication
public class GamingApp {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(GamingApp.class, args);
        
        System.out.println("\n=== Testing Scopes ===");
        
        // Test Singleton
        PlayStation ps1 = context.getBean(PlayStation.class);
        PlayStation ps2 = context.getBean(PlayStation.class);
        System.out.println("Same PlayStation? " + (ps1 == ps2));
        
        // Test Prototype
        Game game1 = context.getBean(Game.class);
        Game game2 = context.getBean(Game.class);
        System.out.println("Same Game? " + (game1 == game2));
        
        // Play games
        Gamer gamer1 = context.getBean(Gamer.class);
        Gamer gamer2 = context.getBean(Gamer.class);
        
        gamer1.playGame();
        gamer2.playGame();
    }
}
```

### Scope Comparison Table

| Scope | Instances | When Created | When Destroyed | Use Case |
|-------|-----------|--------------|----------------|----------|
| **Singleton** | 1 per app | At startup | At shutdown | Services, Repositories |
| **Prototype** | New each time | When requested | When no longer referenced | DTOs, Entities |
| **Request** | 1 per request | Per HTTP request | After request | Request-specific data |
| **Session** | 1 per session | Per user session | When session expires | User preferences |
| **Application** | 1 per app | At startup | At shutdown | Global app data |
| **WebSocket** | 1 per connection | Per WebSocket | When disconnected | Real-time gaming |

### Best Practices

#### 1. Use Singleton for Services
```java
@Service
@Scope("singleton") // Default
public class GamingService {
    // Business logic here
}
```

#### 2. Use Prototype for Stateful Objects
```java
@Component
@Scope("prototype")
public class GameSession {
    private String sessionId;
    // Session-specific data
}
```

#### 3. Use Request for Request-Specific Data
```java
@Component
@Scope("request")
public class RequestContext {
    private String requestId;
    // Request-specific data
}
```

#### 4. Use Session for User Data
```java
@Component
@Scope("session")
public class UserPreferences {
    private String theme;
    private String language;
    // User-specific data
}
```

### Key Points to Remember:

- **Singleton** = One instance for the entire application
- **Prototype** = New instance every time
- **Request** = New instance per HTTP request
- **Session** = One instance per user session
- **Application** = One instance per web application
- **WebSocket** = One instance per WebSocket connection

---

## 5. Circular Dependency Handling

### What is Circular Dependency?

**Circular Dependency Definition:** When two or more beans depend on each other, creating a cycle that Spring cannot resolve.

**Simple Analogy:** Like two friends who both want to borrow money from each other at the same time - neither can help the other because they're both waiting for the other to help first.

### Example of Circular Dependency

```java
// This creates a circular dependency!
@Component
public class PlayStation {
    private final Gamer gamer;
    
    public PlayStation(Gamer gamer) {
        this.gamer = gamer;
    }
    
    public void startGame() {
        System.out.println("🎮 PlayStation started game!");
        gamer.playGame();
    }
}

@Component
public class Gamer {
    private final PlayStation playStation;
    
    public Gamer(PlayStation playStation) {
        this.playStation = playStation;
    }
    
    public void playGame() {
        System.out.println("👾 Gamer is playing!");
        playStation.startGame();
    }
}
```

**Problem:** PlayStation needs Gamer, but Gamer needs PlayStation. Spring can't decide which to create first!

### Solution 1: Use @Lazy Annotation

**@Lazy Definition:** Delays the creation of a bean until it's actually needed.

```java
@Component
public class PlayStation {
    private final Gamer gamer;
    
    public PlayStation(@Lazy Gamer gamer) {
        this.gamer = gamer;
    }
    
    public void startGame() {
        System.out.println("🎮 PlayStation started game!");
        gamer.playGame();
    }
}

@Component
public class Gamer {
    private final PlayStation playStation;
    
    public Gamer(PlayStation playStation) {
        this.playStation = playStation;
    }
    
    public void playGame() {
        System.out.println("👾 Gamer is playing!");
        playStation.startGame();
    }
}

@SpringBootApplication
public class GamingApp {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(GamingApp.class, args);
        
        Gamer gamer = context.getBean(Gamer.class);
        gamer.playGame();
    }
}
```

**How it works:**
1. Spring creates PlayStation first
2. Gamer is created with @Lazy, so it's not created yet
3. When PlayStation calls gamer.playGame(), Spring creates Gamer
4. No circular dependency!

### Solution 2: Use Setter Injection

**Setter Injection Definition:** Inject dependencies through setter methods instead of constructors.

```java
@Component
public class PlayStation {
    private Gamer gamer;
    
    public PlayStation() {
        System.out.println("🏭 PlayStation created!");
    }
    
    @Autowired
    public void setGamer(Gamer gamer) {
        this.gamer = gamer;
    }
    
    public void startGame() {
        System.out.println("🎮 PlayStation started game!");
        gamer.playGame();
    }
}

@Component
public class Gamer {
    private PlayStation playStation;
    
    public Gamer() {
        System.out.println("👤 Gamer created!");
    }
    
    @Autowired
    public void setPlayStation(PlayStation playStation) {
        this.playStation = playStation;
    }
    
    public void playGame() {
        System.out.println("👾 Gamer is playing!");
        playStation.startGame();
    }
}

@SpringBootApplication
public class GamingApp {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(GamingApp.class, args);
        
        Gamer gamer = context.getBean(Gamer.class);
        gamer.playGame();
    }
}
```

**How it works:**
1. Spring creates both objects first
2. Then Spring injects dependencies through setter methods
3. No circular dependency!

### Solution 3: Use @PostConstruct

**@PostConstruct Definition:** Initialize dependencies after both objects are created.

```java
@Component
public class PlayStation {
    private Gamer gamer;
    
    public PlayStation() {
        System.out.println("🏭 PlayStation created!");
    }
    
    @PostConstruct
    public void initialize() {
        // Get Gamer from Spring context
        this.gamer = ApplicationContextProvider.getBean(Gamer.class);
    }
    
    public void startGame() {
        System.out.println("🎮 PlayStation started game!");
        gamer.playGame();
    }
}

@Component
public class Gamer {
    private PlayStation playStation;
    
    public Gamer() {
        System.out.println("👤 Gamer created!");
    }
    
    @PostConstruct
    public void initialize() {
        // Get PlayStation from Spring context
        this.playStation = ApplicationContextProvider.getBean(PlayStation.class);
    }
    
    public void playGame() {
        System.out.println("👾 Gamer is playing!");
        playStation.startGame();
    }
}

// Helper class to get beans from context
@Component
public class ApplicationContextProvider implements ApplicationContextAware {
    private static ApplicationContext context;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }
    
    public static <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }
}

@SpringBootApplication
public class GamingApp {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(GamingApp.class, args);
        
        Gamer gamer = context.getBean(Gamer.class);
        gamer.playGame();
    }
}
```

### Solution 4: Redesign Architecture (Best Practice)

**Redesign Definition:** Change the design to avoid circular dependencies altogether.

```java
// Instead of circular dependency, use a service layer
@Service
public class GamingService {
    private final PlayStation playStation;
    private final Gamer gamer;
    
    public GamingService(PlayStation playStation, Gamer gamer) {
        this.playStation = playStation;
        this.gamer = gamer;
    }
    
    public void startGamingSession() {
        System.out.println("🎮 Starting gaming session!");
        playStation.startGame();
        gamer.playGame();
    }
}

@Component
public class PlayStation {
    public void startGame() {
        System.out.println("🎮 PlayStation started game!");
    }
}

@Component
public class Gamer {
    public void playGame() {
        System.out.println("👾 Gamer is playing!");
    }
}

@SpringBootApplication
public class GamingApp {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(GamingApp.class, args);
        
        GamingService gamingService = context.getBean(GamingService.class);
        gamingService.startGamingSession();
    }
}
```

### Solution 5: Use ApplicationContextAware

**ApplicationContextAware Definition:** Allows a bean to access the Spring application context.

```java
@Component
public class PlayStation implements ApplicationContextAware {
    private ApplicationContext context;
    private Gamer gamer;
    
    public PlayStation() {
        System.out.println("🏭 PlayStation created!");
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.context = applicationContext;
    }
    
    @PostConstruct
    public void initialize() {
        this.gamer = context.getBean(Gamer.class);
    }
    
    public void startGame() {
        System.out.println("🎮 PlayStation started game!");
        gamer.playGame();
    }
}

@Component
public class Gamer implements ApplicationContextAware {
    private ApplicationContext context;
    private PlayStation playStation;
    
    public Gamer() {
        System.out.println("👤 Gamer created!");
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.context = applicationContext;
    }
    
    @PostConstruct
    public void initialize() {
        this.playStation = context.getBean(PlayStation.class);
    }
    
    public void playGame() {
        System.out.println("👾 Gamer is playing!");
        playStation.startGame();
    }
}
```

### Complete Working Example

```java
// GamingService.java - No circular dependency!
@Service
public class GamingService {
    private final PlayStation playStation;
    private final Gamer gamer;
    
    public GamingService(PlayStation playStation, Gamer gamer) {
        this.playStation = playStation;
        this.gamer = gamer;
    }
    
    public void startGamingSession() {
        System.out.println("🎮 Starting gaming session!");
        playStation.startGame();
        gamer.playGame();
    }
}

// PlayStation.java
@Component
public class PlayStation {
    public void startGame() {
        System.out.println("🎮 PlayStation started game!");
    }
}

// Gamer.java
@Component
public class Gamer {
    public void playGame() {
        System.out.println("👾 Gamer is playing!");
    }
}

// GamingApp.java
@SpringBootApplication
public class GamingApp {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(GamingApp.class, args);
        
        GamingService gamingService = context.getBean(GamingService.class);
        gamingService.startGamingSession();
    }
}
```

### Circular Dependency Solutions Comparison

| Solution | Pros | Cons | When to Use |
|----------|------|------|-------------|
| **@Lazy** | Simple, minimal changes | Can hide design issues | Quick fixes |
| **Setter Injection** | Flexible, easy to understand | Less type-safe | Legacy code |
| **@PostConstruct** | Full control | More complex | Complex scenarios |
| **Redesign** | Best practice, clean code | Requires more work | New development |
| **ApplicationContextAware** | Full control | Tight coupling to Spring | Advanced scenarios |

### Best Practices

#### 1. Avoid Circular Dependencies (Best)
```java
// Good: Use service layer
@Service
public class GamingService {
    private final PlayStation playStation;
    private final Gamer gamer;
    // No circular dependency!
}
```

#### 2. Use @Lazy for Quick Fixes
```java
@Component
public class PlayStation {
    public PlayStation(@Lazy Gamer gamer) {
        // Quick fix for circular dependency
    }
}
```

#### 3. Use Setter Injection for Legacy Code
```java
@Component
public class PlayStation {
    @Autowired
    public void setGamer(Gamer gamer) {
        // Setter injection avoids circular dependency
    }
}
```

#### 4. Redesign for New Code
```java
// Best: Redesign to avoid circular dependencies
@Service
public class GamingService {
    // Service layer coordinates between components
}
```

### Key Points to Remember:

- **Circular dependencies** happen when beans depend on each other
- **@Lazy** is the quickest fix
- **Setter injection** works but is less type-safe
- **Redesign** is the best long-term solution
- **Avoid circular dependencies** in new code
- **Use service layers** to coordinate between components

---

## 6. ApplicationContext vs BeanFactory

### What are ApplicationContext and BeanFactory?

**BeanFactory Definition:** The basic container that provides the fundamental functionality for managing beans.

**ApplicationContext Definition:** An advanced container that extends BeanFactory with additional enterprise features.

**Simple Analogy:** 
- **BeanFactory** = Basic PlayStation (just plays games)
- **ApplicationContext** = PlayStation Pro (plays games + streaming + VR + more features)

### BeanFactory (Basic Container)

**BeanFactory Features:**
- Basic bean creation and management
- Dependency injection
- Bean lifecycle management
- No additional enterprise features

```java
// Using BeanFactory
public class GamingApp {
    public static void main(String[] args) {
        // Create BeanFactory
        BeanFactory factory = new XmlBeanFactory(new ClassPathResource("gaming-config.xml"));
        
        // Get beans
        PlayStation playStation = factory.getBean("playStation", PlayStation.class);
        Gamer gamer = factory.getBean("gamer", Gamer.class);
        
        // Use beans
        gamer.playGame();
    }
}
```

**XML Configuration (gaming-config.xml):**
```xml
<beans xmlns="http://www.springframework.org/schema/beans">
    <bean id="playStation" class="com.example.PlayStation"/>
    <bean id="gamer" class="com.example.Gamer">
        <constructor-arg ref="playStation"/>
    </bean>
</beans>
```

### ApplicationContext (Advanced Container)

**ApplicationContext Features:**
- Everything BeanFactory provides
- Internationalization (i18n)
- Event publishing
- Resource management
- AOP support
- Web application context

```java
// Using ApplicationContext
@SpringBootApplication
public class GamingApp {
    public static void main(String[] args) {
        // Create ApplicationContext
        ApplicationContext context = SpringApplication.run(GamingApp.class, args);
        
        // Get beans
        PlayStation playStation = context.getBean(PlayStation.class);
        Gamer gamer = context.getBean(Gamer.class);
        
        // Use beans
        gamer.playGame();
    }
}
```

### Complete Comparison Example

```java
// PlayStation.java
@Component
public class PlayStation {
    public void startGame() {
        System.out.println("🎮 PlayStation started game!");
    }
}

// Gamer.java
@Component
public class Gamer {
    private final PlayStation playStation;
    
    public Gamer(PlayStation playStation) {
        this.playStation = playStation;
    }
    
    public void playGame() {
        playStation.startGame();
        System.out.println("👾 Gamer is playing!");
    }
}

// BeanFactory Example
public class BeanFactoryExample {
    public static void main(String[] args) {
        System.out.println("=== BeanFactory Example ===");
        
        // Create BeanFactory
        BeanFactory factory = new XmlBeanFactory(new ClassPathResource("gaming-config.xml"));
        
        // Get beans
        PlayStation playStation = factory.getBean("playStation", PlayStation.class);
        Gamer gamer = factory.getBean("gamer", Gamer.class);
        
        // Use beans
        gamer.playGame();
    }
}

// ApplicationContext Example
@SpringBootApplication
public class ApplicationContextExample {
    public static void main(String[] args) {
        System.out.println("=== ApplicationContext Example ===");
        
        // Create ApplicationContext
        ApplicationContext context = SpringApplication.run(ApplicationContextExample.class, args);
        
        // Get beans
        PlayStation playStation = context.getBean(PlayStation.class);
        Gamer gamer = context.getBean(Gamer.class);
        
        // Use beans
        gamer.playGame();
    }
}
```

### Key Differences

| Feature | BeanFactory | ApplicationContext |
|---------|-------------|-------------------|
| **Bean Management** | ✅ Basic | ✅ Advanced |
| **Dependency Injection** | ✅ Yes | ✅ Yes |
| **Bean Lifecycle** | ✅ Yes | ✅ Yes |
| **Internationalization** | ❌ No | ✅ Yes |
| **Event Publishing** | ❌ No | ✅ Yes |
| **Resource Management** | ❌ No | ✅ Yes |
| **AOP Support** | ❌ No | ✅ Yes |
| **Web Support** | ❌ No | ✅ Yes |
| **Annotation Support** | ❌ No | ✅ Yes |
| **Auto Configuration** | ❌ No | ✅ Yes |

---

## 7. Spring Boot Basics

### What is Spring Boot?

**Spring Boot Definition:** A framework that makes it easy to create Spring applications with minimal configuration.

**Simple Analogy:** Like a PlayStation that comes pre-configured with all the games and settings you need - you just plug it in and start playing!

### 1. Auto-configuration

#### @SpringBootApplication

**@SpringBootApplication Definition:** A convenience annotation that combines @Configuration, @EnableAutoConfiguration, and @ComponentScan.

```java
@SpringBootApplication
public class GamingApp {
    public static void main(String[] args) {
        SpringApplication.run(GamingApp.class, args);
    }
}

// This is equivalent to:
@Configuration
@EnableAutoConfiguration
@ComponentScan
public class GamingApp {
    public static void main(String[] args) {
        SpringApplication.run(GamingApp.class, args);
    }
}
```

**What it does:**
- **@Configuration:** Marks this class as a configuration class
- **@EnableAutoConfiguration:** Enables Spring Boot auto-configuration
- **@ComponentScan:** Scans for components in the same package

#### @EnableAutoConfiguration

**@EnableAutoConfiguration Definition:** Automatically configures Spring based on the classpath and your beans.

```java
@SpringBootApplication
public class GamingApp {
    public static void main(String[] args) {
        SpringApplication.run(GamingApp.class, args);
    }
}

@Component
public class PlayStation {
    public void startGame() {
        System.out.println("🎮 PlayStation started game!");
    }
}

@Component
public class Gamer {
    private final PlayStation playStation;
    
    public Gamer(PlayStation playStation) {
        this.playStation = playStation;
    }
    
    public void playGame() {
        playStation.startGame();
        System.out.println("👾 Gamer is playing!");
    }
}
```

**What Spring Boot does automatically:**
- Creates a web server (Tomcat)
- Sets up Spring MVC
- Configures JSON serialization
- Sets up logging
- And much more!

### 2. Spring Boot Starters

**Spring Boot Starters Definition:** Pre-configured dependency sets that provide everything you need for specific functionality.

#### Web Starter
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

```java
@RestController
public class GamingController {
    private final GamingService gamingService;
    
    public GamingController(GamingService gamingService) {
        this.gamingService = gamingService;
    }
    
    @GetMapping("/play")
    public String playGame() {
        return gamingService.startGame();
    }
}

@Service
public class GamingService {
    public String startGame() {
        return "🎮 Game started!";
    }
}
```

#### Data JPA Starter
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

```java
@Entity
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String genre;
    
    // Getters and setters
}

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByGenre(String genre);
}

@Service
public class GameService {
    private final GameRepository gameRepository;
    
    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }
    
    public List<Game> getGamesByGenre(String genre) {
        return gameRepository.findByGenre(genre);
    }
}
```

#### Security Starter
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/play").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic();
        return http.build();
    }
}
```

### 3. application.properties vs application.yml

#### application.properties
```properties
# Server configuration
server.port=8080
server.servlet.context-path=/gaming

# Database configuration
spring.datasource.url=jdbc:h2:mem:gamingdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA configuration
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# Gaming configuration
gaming.console.name=PlayStation
gaming.console.version=5
gaming.multiplayer.enabled=true
```

#### application.yml
```yaml
server:
  port: 8080
  servlet:
    context-path: /gaming

spring:
  datasource:
    url: jdbc:h2:mem:gamingdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

gaming:
  console:
    name: PlayStation
    version: 5
  multiplayer:
    enabled: true
```

**Key Differences:**
- **Properties:** Key-value pairs, more verbose
- **YAML:** Hierarchical structure, more readable
- **Both work:** Choose based on preference

### 4. Externalized Configuration

#### @Value Annotation
```java
@Component
public class GamingConfig {
    
    @Value("${gaming.console.name:PlayStation}")
    private String consoleName;
    
    @Value("${gaming.console.version:5}")
    private int consoleVersion;
    
    @Value("${gaming.multiplayer.enabled:false}")
    private boolean multiplayerEnabled;
    
    public void displayConfig() {
        System.out.println("🎮 Console: " + consoleName);
        System.out.println("📱 Version: " + consoleVersion);
        System.out.println("👥 Multiplayer: " + multiplayerEnabled);
    }
}
```

#### @ConfigurationProperties
```java
@ConfigurationProperties(prefix = "gaming")
@Component
public class GamingProperties {
    
    private Console console = new Console();
    private Multiplayer multiplayer = new Multiplayer();
    
    public static class Console {
        private String name = "PlayStation";
        private int version = 5;
        
        // Getters and setters
    }
    
    public static class Multiplayer {
        private boolean enabled = false;
        private int maxPlayers = 4;
        
        // Getters and setters
    }
    
    // Getters and setters
}

@Service
public class GamingService {
    private final GamingProperties gamingProperties;
    
    public GamingService(GamingProperties gamingProperties) {
        this.gamingProperties = gamingProperties;
    }
    
    public void startGame() {
        System.out.println("🎮 Starting " + gamingProperties.getConsole().getName());
        if (gamingProperties.getMultiplayer().isEnabled()) {
            System.out.println("👥 Multiplayer enabled!");
        }
    }
}
```

### 5. Spring Boot DevTools

**DevTools Definition:** Development tools that provide automatic restart, live reload, and other development features.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

**Features:**
- **Automatic restart:** Restarts app when classes change
- **Live reload:** Refreshes browser automatically
- **Property defaults:** Sensible defaults for development
- **Remote debugging:** Debug remote applications

### 6. Spring Boot Actuator

**Actuator Definition:** Production-ready features for monitoring and managing your application.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```properties
# Actuator configuration
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always
```

```java
@Component
public class GamingHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // Check if gaming system is healthy
        boolean isHealthy = checkGamingSystem();
        
        if (isHealthy) {
            return Health.up()
                .withDetail("gaming", "PlayStation is ready")
                .build();
        } else {
            return Health.down()
                .withDetail("gaming", "PlayStation is not responding")
                .build();
        }
    }
    
    private boolean checkGamingSystem() {
        // Simulate health check
        return true;
    }
}
```

**Available Endpoints:**
- `/actuator/health` - Application health
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics
- `/actuator/env` - Environment properties

### 7. Embedded Servers

**Embedded Server Definition:** Web servers that are included in your application JAR file.

#### Tomcat (Default)
```java
@SpringBootApplication
public class GamingApp {
    public static void main(String[] args) {
        SpringApplication.run(GamingApp.class, args);
    }
}

@RestController
public class GamingController {
    
    @GetMapping("/play")
    public String playGame() {
        return "🎮 Game started on embedded Tomcat!";
    }
}
```

#### Jetty
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
```

### Complete Working Example

```java
// GamingApp.java
@SpringBootApplication
public class GamingApp {
    public static void main(String[] args) {
        SpringApplication.run(GamingApp.class, args);
    }
}

// PlayStation.java
@Component
public class PlayStation {
    private final GamingProperties gamingProperties;
    
    public PlayStation(GamingProperties gamingProperties) {
        this.gamingProperties = gamingProperties;
    }
    
    public void startGame() {
        System.out.println("🎮 " + gamingProperties.getConsole().getName() + " started game!");
    }
}

// GamingController.java
@RestController
public class GamingController {
    private final PlayStation playStation;
    
    public GamingController(PlayStation playStation) {
        this.playStation = playStation;
    }
    
    @GetMapping("/play")
    public String playGame() {
        playStation.startGame();
        return "Game started!";
    }
}

// GamingProperties.java
@ConfigurationProperties(prefix = "gaming")
@Component
public class GamingProperties {
    private Console console = new Console();
    
    public static class Console {
        private String name = "PlayStation";
        private int version = 5;
        
        // Getters and setters
    }
    
    // Getters and setters
}
```

### Key Benefits of Spring Boot:

1. **Auto-configuration:** Minimal configuration needed
2. **Starters:** Pre-configured dependencies
3. **Embedded servers:** No need to deploy to external servers
4. **Production-ready:** Actuator for monitoring
5. **Development-friendly:** DevTools for faster development
6. **Externalized configuration:** Easy to configure for different environments

**That's it! Spring Boot Basics in simple terms!** 🎮

**Ready for the next topic? Say "yes" to proceed to "Property Management".**

