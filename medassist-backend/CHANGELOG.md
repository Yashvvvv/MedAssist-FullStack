# PharmaLens - AI Pharmaceutical Intelligence Platform - Changelog

## [2.0.0] - December 8, 2025

### 🎯 Complete 5-Phase Security Audit & Modernization

This release includes a comprehensive 5-phase audit covering security hardening, performance optimization, code quality improvements, and production-ready Docker deployment support.

---

## 📋 Phase Summary

| Phase | Category | Status |
|-------|----------|--------|
| Phase 1 | Critical Security Fixes | ✅ Complete |
| Phase 2 | High Priority Improvements | ✅ Complete |
| Phase 3 | Performance Optimizations | ✅ Complete |
| Phase 4 | Code Quality Improvements | ✅ Complete |
| Phase 5 | DevOps & Deployment | ✅ Complete |

**Test Results:** 36 tests passing ✅

---

## 🔐 Phase 1: Critical Security Fixes

### JWT Token Validation Hardening
**File:** `src/main/java/com/medassist/auth/service/JwtTokenService.java`
- Fixed potential JWT validation bypass vulnerability
- Added proper exception handling for all JWT parsing scenarios
- Tokens validated with explicit algorithm verification
- Added secure logging for invalid token attempts

### Secure Exception Logging
- Removed sensitive information from error logs
- Stack traces no longer expose internal implementation details
- Proper sanitization of user input in log messages

---

## ⚡ Phase 2: High Priority Improvements

### Custom Exception Hierarchy
New domain-specific exceptions in `com.medassist.common.exception`:

| Exception | HTTP Status | Purpose |
|-----------|-------------|---------|
| `ResourceNotFoundException` | 404 | Base for missing resources |
| `InvalidCredentialsException` | 401 | Authentication failures |
| `InvalidTokenException` | 401 | JWT/verification token issues |
| `UserAlreadyExistsException` | 409 | Registration conflicts |
| `BusinessValidationException` | 400 | Business rule violations |

### Constructor-Based Dependency Injection
Converted **12+ classes** from field injection (`@Autowired`) to constructor injection:

- `JwtTokenService`, `AuthController`, `UserService`
- `PasswordResetService`, `PharmacyService`, `MedicineService`
- `GeminiService`, `CoreDataInitializationService`
- `BackendDataInitializationService`, `RateLimitInterceptor`

**Benefits:** Immutable dependencies, easier testing, fail-fast behavior

### Consolidated Exception Handlers
- Merged duplicate handlers into single `GlobalExceptionHandler`
- Deleted redundant `CoreGlobalExceptionHandler.java`
- Consistent error response format across all endpoints

---

## 🚀 Phase 3: Performance Optimizations

### Database Indexing
Added indexes for frequently queried columns:

```java
// Medicine Entity
@Index(name = "idx_medicine_name", columnList = "name")
@Index(name = "idx_medicine_category", columnList = "category")

// Pharmacy Entity  
@Index(name = "idx_pharmacy_name", columnList = "name")
@Index(name = "idx_pharmacy_city", columnList = "city")

// User Entity
@Index(name = "idx_user_email", columnList = "email", unique = true)
```

### Lazy Loading for Collections
- Changed `FetchType.EAGER` to `FetchType.LAZY` for `Pharmacy.medicines`
- Prevents N+1 query issues
- Reduces memory footprint

### Optional Return Types
Converted null-returning methods to `Optional<>`:
- `MedicineService.findByName()`, `findById()`
- `PharmacyService.findById()`
- Repository query methods

---

## 🧹 Phase 4: Code Quality Improvements

### Lombok Integration
| Annotation | Usage |
|------------|-------|
| `@Slf4j` | All service/controller classes |
| `@RequiredArgsConstructor` | Constructor injection |
| `@Data` | DTOs and entities |
| `@Builder` | Complex object construction |

### Configuration Consolidation
- Merged `SecurityConfig.java` into `CoreSecurityConfig.java`
- Single source of truth for security configuration
- Removed conflicting bean definitions

---

## 🐳 Phase 5: DevOps & Deployment

### New Files Created

| File | Purpose |
|------|---------|
| `Dockerfile` | Multi-stage build with Java 21 |
| `.dockerignore` | Optimized Docker builds |
| `docker-compose.yml` | Full stack deployment |
| `application-prod.properties` | Production configuration |
| `DatabaseHealthIndicator.java` | Custom DB health check |
| `AIServiceHealthIndicator.java` | Custom AI health check |

### Docker Features
- **Multi-stage build** - Smaller final image (~200MB)
- **Non-root user** - Security best practice
- **Health checks** - Container orchestration ready
- **JVM optimization** - Container-aware settings

### Docker Compose Stack
| Service | Port | Purpose |
|---------|------|---------|
| `postgres` | 5432 | PostgreSQL 15 database |
| `medassist-backend` | 8080 | Spring Boot application |
| `redis` | 6379 | Session/cache storage |

### Health Check Endpoints
```bash
GET /actuator/health           # Overall health
GET /actuator/health/liveness  # Kubernetes liveness probe
GET /actuator/health/readiness # Kubernetes readiness probe
```

### Quick Start
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f medassist-backend

# Check health
curl http://localhost:8080/actuator/health
```

---

## [Unreleased] - December 7, 2025

### 🏗️ Major Refactoring: Domain-Driven Clean Architecture

**BREAKING CHANGE**: Complete package structure reorganization to domain-driven design

#### Final Package Structure (Domain-Driven)
```
com.medassist/
├── common/                    # Shared utilities across domains
│   ├── config/               # AsyncConfig, SwaggerConfig, WebMvcConfig, RateLimitConfig
│   ├── dto/                  # ApiResponse, ApiErrorResponse
│   ├── exception/            # Global exception handlers
│   ├── interceptor/          # ApiAccessLogger
│   ├── security/             # SecurityConfig, JwtFilter, JwtEntryPoint
│   └── validation/           # CoordinateValidator, ValidCoordinates
├── auth/                      # Authentication & User Management domain
│   ├── controller/           # AuthenticationController, UserProfileController
│   ├── dto/                  # LoginRequestDto, UserRegistrationDto, etc.
│   ├── entity/               # User, Role, Permission, PasswordResetToken
│   ├── integration/          # JWT utilities, service clients
│   ├── repository/           # UserRepository, RoleRepository, etc.
│   └── service/              # AuthenticationService, JwtTokenService, EmailService
├── medicine/                  # Medicine Management domain
│   ├── controller/           # MedicineController
│   ├── dto/                  # MedicineAnalysisRequest/Response
│   ├── entity/               # Medicine
│   ├── mapper/               # MedicineMapper
│   ├── repository/           # MedicineRepository
│   └── service/              # MedicineService, MedicineAvailabilityService
├── pharmacy/                  # Pharmacy & Location domain
│   ├── controller/           # PharmacyController, PharmacyLocationController
│   ├── dto/                  # PharmacyLocationRequest/Response
│   ├── entity/               # Pharmacy
│   ├── mapper/               # PharmacyMapper
│   ├── repository/           # PharmacyRepository
│   └── service/              # PharmacyService, GoogleMapsService, PharmacyLocationService
├── ai/                        # AI & Machine Learning domain
│   ├── controller/           # MedicineAIController
│   ├── dto/                  # GeminiRequest, GeminiResponse
│   └── service/              # GeminiAIService, MedicineAIService, ImageProcessingService
└── MedassistApplication.java
```

**Benefits:**
- ✅ **Microservice-ready**: Each domain can be extracted to its own service
- ✅ **Better encapsulation**: Business logic contained within domain boundaries
- ✅ **Clearer ownership**: Easy to understand which team owns what
- ✅ **Scalable architecture**: Add new domains without touching existing code
- ✅ **Testable in isolation**: Domain-specific integration tests

---

### 🔒 Security Improvements

#### 1. JWT Secret Key Security
**File:** `JwtTokenService.java`
- **Before:** Hardcoded JWT secret key in source code
- **After:** JWT secret loaded from environment variable `JWT_SECRET_KEY`
- **Impact:** Critical security fix - secrets no longer in version control

```java
// Before (INSECURE)
private String jwtSecret = "hardcoded-secret-key";

// After (SECURE)
@Value("${jwt.secret:${JWT_SECRET_KEY:}}")
private String jwtSecret;

@PostConstruct
public void validateConfiguration() {
    if (jwtSecret == null || jwtSecret.isBlank()) {
        throw new IllegalStateException("JWT_SECRET_KEY environment variable must be set");
    }
}
```

#### 2. CORS Configuration Centralization
**File:** `CoreSecurityConfig.java`
- **Before:** CORS configuration duplicated in multiple files, hardcoded origins
- **After:** Centralized CORS config with environment variable support
- **Property:** `CORS_ALLOWED_ORIGINS` (comma-separated list)

```java
@Value("${cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
private String allowedOrigins;
```

#### 3. Image Upload Security - Magic Bytes Validation
**File:** `ImageProcessingService.java`
- **Before:** Only file extension validation (easily bypassed)
- **After:** Binary magic bytes validation for actual file content

```java
private boolean isValidImageMagicBytes(byte[] imageData) {
    if (imageData == null || imageData.length < 8) return false;
    
    // JPEG: FF D8 FF
    if (imageData[0] == (byte) 0xFF && imageData[1] == (byte) 0xD8 && imageData[2] == (byte) 0xFF) {
        return true;
    }
    // PNG: 89 50 4E 47 0D 0A 1A 0A
    if (imageData[0] == (byte) 0x89 && imageData[1] == (byte) 0x50 && ...) {
        return true;
    }
    // ... GIF, WebP support
    return false;
}
```

#### 4. Token Blacklist Service
**File:** `TokenBlacklistService.java` (NEW)
- Caffeine cache-based token blacklist for logout/revocation
- Automatic expiry based on token TTL
- Memory-efficient implementation

```java
@Service
public class TokenBlacklistService {
    private final Cache<String, Boolean> blacklistedTokens;
    
    public void blacklistToken(String token, long expirationTimeMillis) { ... }
    public boolean isBlacklisted(String token) { ... }
}
```

#### 5. Rate Limiter Memory Leak Fix
**File:** `RateLimitService.java`
- **Before:** Unbounded ConcurrentHashMap could grow indefinitely
- **After:** Caffeine cache with automatic eviction

```java
// Before (MEMORY LEAK)
private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

// After (BOUNDED)
private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
    .maximumSize(100_000)
    .expireAfterAccess(Duration.ofHours(1))
    .build();
```

---

### 🛡️ Input Validation & Null Safety

#### 1. DTO Validation Annotations
**Files:** Multiple DTOs
- Added `@NotBlank`, `@Size`, `@Email`, `@Pattern` annotations
- Server-side validation for all user inputs

```java
public class LoginRequestDto {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
```

#### 2. Null-Safe Service Methods
**Files:** `MedicineService.java`, `PharmacyService.java`
- Added null checks before entity operations
- Defensive programming patterns

```java
public Medicine updateMedicine(Long id, Medicine medicineDetails) {
    if (medicineDetails == null) {
        return null;
    }
    return medicineRepository.findById(id)
        .map(medicine -> {
            if (medicineDetails.getName() != null) {
                medicine.setName(medicineDetails.getName());
            }
            // ... other null-safe updates
            return medicineRepository.save(medicine);
        })
        .orElse(null);
}
```

---

### ⚡ Performance Improvements

#### 1. Database Indexes
**File:** `Medicine.java`, `Pharmacy.java`
- Added composite indexes for frequently queried fields

```java
@Table(name = "medicines", indexes = {
    @Index(name = "idx_medicine_name", columnList = "name"),
    @Index(name = "idx_medicine_category", columnList = "category"),
    @Index(name = "idx_medicine_generic_name", columnList = "genericName"),
    @Index(name = "idx_medicine_manufacturer", columnList = "manufacturer")
})
```

#### 2. Async Configuration
**File:** `AsyncConfig.java`
- Proper thread pool configuration for async operations
- Custom exception handling for async tasks

```java
@Bean(name = "taskExecutor")
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("MedAssist-Async-");
    executor.initialize();
    return executor;
}
```

---

### 🐛 Bug Fixes

1. **Fixed potential NPE in JwtAuthenticationFilter** - Added null checks for authentication context
2. **Fixed GoogleMapsService API key validation** - Graceful handling when API key not configured
3. **Fixed PharmacyService null pointer** - Safe navigation in update methods
4. **Fixed CoreDataInitializationService** - Null-safe sample data initialization

---

### 📊 Test Coverage

| Test Class | Tests | Status |
|------------|-------|--------|
| MedicineControllerTest | 15 | ✅ All Pass |
| MedAssistIntegrationTest | 8 | ✅ All Pass |
| MedassistApplicationTests | 1 | ✅ All Pass |
| MedicineServiceTest | 12 | ✅ All Pass |
| **Total** | **36** | **✅ 100% Pass** |

---

### 📦 Dependencies

- **Spring Boot:** 3.4.4
- **Java:** 21
- **jjwt:** 0.13.0
- **Caffeine Cache:** (via Spring Boot)
- **PostgreSQL:** Runtime
- **H2:** Test scope

---

### 🔧 Configuration Changes

#### New Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `JWT_SECRET_KEY` | ✅ Yes | - | JWT signing secret (min 256-bit) |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:3000,http://localhost:8080` | Comma-separated allowed origins |
| `GOOGLE_MAPS_API_KEY` | No | - | Google Maps API key |
| `GEMINI_API_KEY` | No | - | Google Gemini AI API key |

#### application.properties additions
```properties
# JWT Configuration
jwt.secret=${JWT_SECRET_KEY:}

# CORS Configuration  
cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:8080}
```

---

### 📁 File Statistics

- **Total Source Files:** 81
- **Total Test Files:** 4
- **Lines of Code Changed:** 390+ insertions, 394 deletions
- **Files Renamed/Moved:** 80+

---

### ✨ Code Quality Improvements (Phase 2)

#### 1. Lombok Integration - Reduced Boilerplate
**Files:** Entity classes, DTOs

Added Lombok dependency to eliminate boilerplate code:

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

**Converted Entities:**
- `Medicine.java` - `@Getter`, `@Setter`, `@NoArgsConstructor`
- `Pharmacy.java` - `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`
- `User.java` - `@Getter`, `@Setter`, `@NoArgsConstructor`
- `Role.java` - `@Getter`, `@Setter`, `@NoArgsConstructor`
- `Permission.java` - `@Getter`, `@Setter`, `@NoArgsConstructor`
- `PasswordResetToken.java` - `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`

**Converted Auth DTOs:**
- `LoginRequestDto` - `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- `UserRegistrationDto` - `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- `AuthenticationResponseDto` - `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- `ChangePasswordDto` - `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`
- `UserProfileUpdateDto` - `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- `TokenRefreshDto` - `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`
- `PasswordResetRequestDto` - `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`
- `PasswordResetConfirmDto` - `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`
- `HealthcareProviderRegistrationDto` - `@Data`, `@EqualsAndHashCode(callSuper = true)`

**Benefits:**
- Reduced ~500 lines of boilerplate getter/setter/constructor code
- Cleaner, more readable entity definitions
- Consistent code style across all entities
- Builder pattern support for complex object construction

#### 2. API Versioning - `/api/v1/` Prefix
**Files:** All controllers, SecurityConfig

Added API versioning to all endpoints for better backward compatibility:

**Updated Controllers:**
| Controller | Old Path | New Path |
|------------|----------|----------|
| `AuthenticationController` | `/api/auth` | `/api/v1/auth` |
| `MedicineController` | `/api/medicines` | `/api/v1/medicines` |
| `PharmacyController` | `/api/pharmacies` | `/api/v1/pharmacies` |
| `PharmacyLocationController` | `/api/pharmacies/location` | `/api/v1/pharmacies/location` |
| `MedicineAIController` | `/api/ai/medicine` | `/api/v1/ai/medicine` |
| `UserProfileController` | `/api/profile` | `/api/v1/profile` |
| `HealthCheckController` | `/api/health` | `/api/v1/health` |
| `JwtHealthController` | `/api/jwt` | `/api/v1/jwt` |

**Security Config Updates:**
```java
// Updated path matchers for versioned API
.requestMatchers("/api/v1/auth/**").permitAll()
.requestMatchers("/api/v1/medicines/**").permitAll()
.requestMatchers("/api/v1/pharmacies/**").permitAll()
.requestMatchers("/api/v1/health/**").permitAll()
.requestMatchers("/api/v1/jwt/**").permitAll()
```

**Benefits:**
- Future-proof API design
- Ability to support multiple API versions simultaneously
- Clean deprecation path for breaking changes
- Industry standard REST API practice

#### 3. SLF4J Logging - Replaced System.out.println
**Files:** `CoreDataInitializationService.java`, `BackendGlobalExceptionHandler.java`

Replaced `System.out.println` with proper SLF4J logging:

```java
// Before
System.out.println("Sample medicines initialized successfully!");

// After
@Slf4j
public class CoreDataInitializationService {
    // ...
    log.info("Sample medicines initialized successfully!");
}
```

**Benefits:**
- Configurable log levels (DEBUG, INFO, WARN, ERROR)
- Log output to files with rotation
- Structured logging support
- Better production debugging
- Consistent logging format across application

---

### 🚀 Migration Guide

If upgrading from a previous version:

1. **Set Environment Variables:**
   ```bash
   export JWT_SECRET_KEY="your-256-bit-secret-key-here"
   export CORS_ALLOWED_ORIGINS="https://your-frontend-domain.com"
   ```

2. **Update Import Statements** (if using this as a library):
   ```java
   // Old (layer-based)
   import com.medassist.entity.Medicine;
   import com.medassist.entity.User;
   import com.medassist.service.MedicineService;
   
   // New (domain-driven)
   import com.medassist.medicine.entity.Medicine;
   import com.medassist.auth.entity.User;
   import com.medassist.medicine.service.MedicineService;
   ```

3. **Update API Endpoint URLs:**
   ```
   # Old endpoints
   /api/auth/login
   /api/medicines
   /api/pharmacies
   
   # New versioned endpoints
   /api/v1/auth/login
   /api/v1/medicines
   /api/v1/pharmacies
   ```

4. **Main Application Class:**
   - `MedassistBackendApplication` → `MedassistApplication`

---

### 📝 Git Commits

```
a1da7ad refactor: Domain-driven package structure (clean architecture)
a66b014 docs: Add comprehensive CHANGELOG documenting all improvements
a455ad3 chore: Remove remaining old core/mapper files
9a454a4 refactor: Clean architecture - merge core and medassist_backend into unified com.medassist package
6813893 fix: Security and code quality improvements
```

---

## Authors

- Code Review & Improvements by GitHub Copilot
- Original codebase by MedAssist Team
