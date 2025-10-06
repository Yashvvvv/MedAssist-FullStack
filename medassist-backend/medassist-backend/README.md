# MedAssist Authentication Service

A comprehensive Spring Boot authentication microservice for the MedAssist medical application, featuring JWT-based authentication, role-based access control, healthcare provider verification, and enterprise-grade security features.

## 🏥 Overview

The MedAssist-Auth service is a dedicated authentication microservice that handles all user management, authentication, and authorization for the MedAssist medical platform. It follows enterprise architecture patterns similar to those used by companies like Netflix and Amazon, where identity services are separated from main application logic.

## ✨ Features

### Core Authentication
- ✅ **User Registration & Login** with JWT tokens
- ✅ **Email Verification** workflows with professional HTML templates
- ✅ **Password Reset** functionality with secure time-limited tokens
- ✅ **Healthcare Provider Registration** with specialized verification process
- ✅ **Role-Based Access Control (RBAC)** with fine-grained permissions
- ✅ **BCrypt Password Encryption** for maximum security
- ✅ **Separate Authentication Database** for service isolation
- ✅ **JWT Access & Refresh Tokens** for stateless authentication

### Security Features
- JWT-based stateless authentication
- Role-based endpoint protection
- Healthcare provider license verification
- Email verification for account activation
- Secure password reset with time-limited tokens
- CORS configuration for frontend integration
- Input validation and sanitization
- Protection against common security vulnerabilities

### User Types & Roles
- **USER**: Basic application access with standard permissions
- **HEALTHCARE_PROVIDER**: Healthcare provider with extended medical features
- **VERIFIED_HEALTHCARE_PROVIDER**: Verified healthcare provider with full medical access
- **ADMIN**: System administrator with complete access

### Email Templates
- Professional HTML email templates for verification
- Password reset email with security guidelines
- Healthcare provider verification confirmation
- Responsive design for all devices

## 🏗️ Architecture

### Technology Stack
- **Java 21** - Latest LTS version
- **Spring Boot 3.5.3** - Modern framework
- **Spring Security 6.5.1** - Advanced security features
- **JWT (JJWT 0.11.5)** - Stateless authentication
- **PostgreSQL** - Dedicated authentication database
- **Maven** - Dependency management
- **Thymeleaf** - Email templating
- **BCrypt** - Password hashing

### Database Schema
```sql
-- Core tables automatically created by Hibernate
users (id, username, email, password_hash, first_name, last_name, phone_number, 
       is_verified, is_enabled, is_healthcare_provider, provider_verified,
       license_number, medical_specialty, hospital_affiliation, created_at, updated_at)

roles (id, name, description)
permissions (id, name, description)
user_roles (user_id, role_id)
role_permissions (role_id, permission_id)
verification_tokens (id, token, user_id, expires_at, verified_at, created_at)
password_reset_tokens (id, token, user_id, expires_at, used_at, created_at)
```

### Service Layer Architecture
- **AuthenticationService**: Core authentication business logic
- **JwtTokenService**: JWT token generation, validation, and management
- **EmailService**: Email notifications with HTML templates
- **UserDetailsServiceImpl**: Spring Security integration
- **DataInitializationService**: Automatic setup of roles and permissions

## 🚀 Quick Start

### Prerequisites
- Java 21 or higher
- PostgreSQL 12 or higher
- Maven 3.6 or higher
- SMTP server access (Gmail recommended)

### 1. Database Setup
```bash
# Create PostgreSQL database
psql -U postgres -f src/main/resources/db/setup.sql

# Or manually create database
psql -U postgres -d postgres -c "CREATE DATABASE medassist_auth;"
psql -U postgres -d postgres -c "CREATE USER medassist_user WITH PASSWORD 'medassist_password';"
psql -U postgres -d postgres -c "GRANT ALL PRIVILEGES ON DATABASE medassist_auth TO medassist_user;"
```

### 2. Environment Configuration
Update `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/medassist_auth
spring.datasource.username=medassist_user
spring.datasource.password=your_secure_password

# JWT Configuration (Change in production!)
jwt.secret=your-super-secret-jwt-key-min-256-bits
jwt.expiration=86400000
jwt.refresh-expiration=604800000

# Email Configuration
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password

# Application Configuration
medassist.app.base-url=http://localhost:8080
```

### 3. Build and Run
```bash
# Navigate to project directory
cd medassist-backend

# Clean and compile
./mvnw clean compile

# Run the application
./mvnw spring-boot:run

# Or build JAR and run
./mvnw clean package
java -jar target/medassist-backend-0.0.1-SNAPSHOT.jar
```

The service will start on `http://localhost:8080` with automatic database table creation.

## 📚 API Documentation

### Authentication Endpoints

#### Register Standard User
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890"
}
```

#### Register Healthcare Provider
```http
POST /api/auth/register-healthcare-provider
Content-Type: application/json

{
  "username": "dr_smith",
  "email": "dr.smith@hospital.com",
  "password": "SecurePass123!",
  "firstName": "Dr. Sarah",
  "lastName": "Smith",
  "phoneNumber": "+1234567890",
  "licenseNumber": "MD123456789",
  "medicalSpecialty": "Cardiology",
  "hospitalAffiliation": "General Hospital"
}
```

#### User Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "john_doe",
  "password": "SecurePass123!"
}

Response:
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "isVerified": true,
    "roles": ["USER"],
    "permissions": ["USER_ACCESS"]
  }
}
```

#### Email Verification
```http
GET /api/auth/verify-email?token=verification-token-here
```

#### Password Reset Request
```http
POST /api/auth/forgot-password
Content-Type: application/json

{
  "email": "john@example.com"
}
```

#### Password Reset Confirmation
```http
POST /api/auth/reset-password
Content-Type: application/json

{
  "token": "reset-token-here",
  "newPassword": "NewSecurePass123!"
}
```

#### Token Refresh
```http
POST /api/auth/refresh-token
Content-Type: application/json

{
  "refreshToken": "your-refresh-token-here"
}
```

#### Change Password (Authenticated)
```http
POST /api/auth/change-password
Authorization: Bearer your-access-token
Content-Type: application/json

{
  "currentPassword": "CurrentPass123!",
  "newPassword": "NewSecurePass123!"
}
```

#### Get Current User Info (Authenticated)
```http
GET /api/auth/me
Authorization: Bearer your-access-token
```

### Role-Based Access Control

#### Permission Levels
- **USER_ACCESS**: Basic application features
- **HEALTHCARE_ACCESS**: Healthcare provider features
- **VERIFIED_HEALTHCARE_ACCESS**: Verified healthcare provider features
- **ADMIN_ACCESS**: Administrative features
- **READ_USERS, WRITE_USERS, DELETE_USERS**: User management
- **VIEW_MEDICAL_DATA, EDIT_MEDICAL_DATA**: Medical data access
- **PRESCRIBE_MEDICATION**: Prescription capabilities

#### Protected Endpoints
```http
GET /api/user/**          # Requires USER_ACCESS
GET /api/healthcare/**    # Requires HEALTHCARE_ACCESS
GET /api/verified-healthcare/** # Requires VERIFIED_HEALTHCARE_ACCESS
GET /api/admin/**         # Requires ADMIN_ACCESS
```

## 🧪 Testing

### Manual Testing Commands
```bash
# Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "TestPass123!",
    "firstName": "Test",
    "lastName": "User",
    "phoneNumber": "+1234567890"
  }'

# Login with the user
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "testuser",
    "password": "TestPass123!"
  }'

# Register healthcare provider
curl -X POST http://localhost:8080/api/auth/register-healthcare-provider \
  -H "Content-Type: application/json" \
  -d '{
    "username": "dr_test",
    "email": "doctor@test.com",
    "password": "DocPass123!",
    "firstName": "Dr. Test",
    "lastName": "Provider",
    "phoneNumber": "+1234567890",
    "licenseNumber": "MD123456789",
    "medicalSpecialty": "General Medicine",
    "hospitalAffiliation": "Test Hospital"
  }'

# Test authenticated endpoint
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN_HERE"
```

### Unit Testing
```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=AuthenticationControllerTest

# Run tests with coverage
./mvnw clean test jacoco:report
```

## 🔐 Security Configuration

### Password Requirements
- Minimum 8 characters
- At least one uppercase letter (A-Z)
- At least one lowercase letter (a-z)
- At least one number (0-9)
- At least one special character (@$!%*?&)

### JWT Token Configuration
- **Access Token**: 24 hours validity, used for API authentication
- **Refresh Token**: 7 days validity, used to obtain new access tokens
- **Algorithm**: HS256 (HMAC SHA-256)
- **Minimum Secret Length**: 256 bits

### Security Headers
- CORS properly configured
- CSRF protection for stateful operations
- XSS protection enabled
- Content Security Policy headers

## 🌐 Frontend Integration

### Authentication Flow
1. User registers/logs in via API
2. Frontend receives JWT tokens
3. Store tokens securely (httpOnly cookies recommended)
4. Include access token in Authorization header
5. Implement token refresh logic
6. Handle authentication errors gracefully

### JavaScript Integration Example
```javascript
// Authentication service
class AuthService {
  constructor() {
    this.baseURL = 'http://localhost:8080/api/auth';
  }

  async login(credentials) {
    const response = await fetch(`${this.baseURL}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(credentials)
    });
    
    if (response.ok) {
      const data = await response.json();
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken);
      return data;
    }
    throw new Error('Login failed');
  }

  async makeAuthenticatedRequest(url, options = {}) {
    const token = localStorage.getItem('accessToken');
    const response = await fetch(url, {
      ...options,
      headers: {
        ...options.headers,
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (response.status === 401) {
      await this.refreshToken();
      return this.makeAuthenticatedRequest(url, options);
    }
    return response;
  }

  async refreshToken() {
    const refreshToken = localStorage.getItem('refreshToken');
    // Implementation for token refresh
  }
}
```

## 📧 Email Configuration

### Gmail Setup (Recommended)
1. Enable 2-factor authentication
2. Generate App Password
3. Use App Password in configuration

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-16-character-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### Email Templates
- **Verification Email**: Professional welcome email with verification link
- **Password Reset**: Security-focused email with reset instructions
- **Healthcare Provider**: Confirmation email for provider applications

## 🚀 Production Deployment

### Environment Variables
```bash
export JWT_SECRET=your-production-jwt-secret-min-256-bits
export DB_PASSWORD=your-production-db-password
export MAIL_USERNAME=your-production-email@domain.com
export MAIL_PASSWORD=your-production-email-password
export SPRING_PROFILES_ACTIVE=prod
```

### Production Build
```bash
# Build production JAR
./mvnw clean package -Pprod

# Run with production profile
java -jar target/medassist-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Docker Deployment
```dockerfile
FROM openjdk:21-jre-slim
COPY target/medassist-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 📊 Monitoring & Health

### Health Check Endpoints
```http
GET /actuator/health          # Application health
GET /actuator/info           # Application info
GET /actuator/metrics        # Application metrics
```

### Logging Configuration
- Structured logging with JSON format
- Different log levels for different environments
- Security event logging
- Performance monitoring

## 🔧 Configuration Reference

### Complete application.properties
```properties
# Application
spring.application.name=medassist-auth
server.port=8080

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/medassist_auth
spring.datasource.username=medassist_user
spring.datasource.password=${DB_PASSWORD:medassist_password}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true

# JWT
jwt.secret=${JWT_SECRET:medassist-super-secret-key-change-in-production}
jwt.expiration=86400000
jwt.refresh-expiration=604800000

# Email
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME:your-email@gmail.com}
spring.mail.password=${MAIL_PASSWORD:your-app-password}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Application URLs
medassist.app.base-url=${BASE_URL:http://localhost:8080}
medassist.app.verification-token-expiration=86400000
medassist.app.reset-token-expiration=3600000

# Actuator
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 🆘 Support

For issues and questions:
1. Check the application logs
2. Verify database connectivity
3. Confirm email server configuration
4. Review JWT token configuration
5. Test with provided curl commands

## 📈 Roadmap

- [ ] OAuth2 integration (Google, Apple)
- [ ] Multi-factor authentication (MFA)
- [ ] Rate limiting and throttling
- [ ] Advanced audit logging
- [ ] WebSocket support for real-time notifications
- [ ] Kubernetes deployment configuration
- [ ] API versioning support

---

Built with ❤️ for the MedAssist healthcare platform.
