# Tech Stack & Build

## Runtime

- Java 17
- Spring Boot 3.2.5
- MySQL 8.0 (via HikariCP connection pool)
- Redis 6.x (via Lettuce)

## Frameworks & Libraries

| Dependency | Purpose |
|-----------|---------|
| MyBatis 3.0.3 (spring-boot-starter) | ORM / SQL mapping |
| Spring Validation | Request validation (`@Valid`, `@NotNull`, etc.) |
| Spring AOP | Cross-cutting concerns |
| Spring Data Redis | Caching, distributed locks, idempotency keys, nonce storage |
| Lombok | Boilerplate reduction (`@Data`, `@Getter`, `@Slf4j`, etc.) |
| Jackson + JSR310 module | JSON serialization, Java 8+ date/time support |
| jqwik 1.8.4 | Property-based testing |
| JUnit 5 (via spring-boot-starter-test) | Unit and integration testing |

## Build System

Maven 3.x with `spring-boot-maven-plugin`.

### Common Commands

以下命令均在 `parking-service/` 目录下执行：

```bash
cd parking-service

# Compile
mvn compile

# Run tests
mvn test

# Package (skip tests)
mvn package -DskipTests

# Run application
mvn spring-boot:run

# Clean build
mvn clean install
```

## Configuration

- Main config: `parking-service/src/main/resources/application.yml`
- Logging: `parking-service/src/main/resources/logback-spring.xml` (includes `requestId` in MDC pattern)
- MyBatis mapper XMLs: `parking-service/src/main/resources/mapper/**/*.xml`
- MyBatis maps `snake_case` DB columns to `camelCase` Java fields automatically

## Key Config Details

- Server port: 8080
- Datasource: `parking_db` on localhost:3306
- Redis: localhost:6379, database 0
- Jackson: non-null inclusion, `yyyy-MM-dd HH:mm:ss` date format, Asia/Shanghai timezone
- Scheduling enabled (`@EnableScheduling`) for cron tasks
