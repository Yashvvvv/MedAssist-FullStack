# PharmaLens - AI Pharmaceutical Intelligence Platform

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()

A comprehensive Spring Boot backend API for the PharmaLens platform, featuring AI-powered medicine recognition, pharmacy location services, JWT-based authentication, and healthcare provider management.

## 📑 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Architecture](#️-architecture)
- [Quick Start](#-quick-start)
- [Docker Deployment](#-docker-deployment)
- [API Documentation](#-api-documentation)
- [Security](#-security-configuration)
- [Testing](#-testing)
- [Monitoring](#-monitoring--health)
- [Configuration](#-configuration-reference)
- [Contributing](#-contributing)
- [License](#-license)

## 🏥 Overview

PharmaLens is a full-featured AI-powered pharmaceutical intelligence platform API that provides:
- **AI-Powered Medicine Analysis** - Image and text-based medicine identification using Google Gemini AI
- **Pharmacy Location Services** - Find nearby pharmacies with Google Maps integration
- **Medicine Database Management** - Comprehensive CRUD operations for medicine catalog
- **Secure Authentication** - JWT-based authentication with role-based access control
- **Healthcare Provider Management** - Specialized registration and verification for medical professionals

## ✨ Features

### 🤖 AI Medicine Recognition
- **Text Analysis**: Identify medicines by name or description using Gemini AI
- **Image Analysis**: Upload medicine images for AI-powered identification
- **Combined Analysis**: Use both text and image for enhanced accuracy
- **Caching**: Caffeine-based caching for improved performance

### 💊 Medicine Management
- Full CRUD operations for medicine catalog
- Advanced search by name, generic name, manufacturer, category, and form
- Comprehensive search with multiple filters
- Database indexing for optimized queries

### 🏪 Pharmacy Services
- **Location-based Search**: Find pharmacies near coordinates with configurable radius
- **Google Maps Integration**: Real-time pharmacy location data
- **Feature Filtering**: 24-hour, delivery, drive-through, consultation services
- **Comprehensive Search**: By name, city, state, zip code, or chain

### 🔐 Authentication & Security
- **JWT Authentication**: Access and refresh token management
- **Role-Based Access Control (RBAC)**: USER, HEALTHCARE_PROVIDER, VERIFIED_HEALTHCARE_PROVIDER, ADMIN
- **Email Verification**: Professional HTML email templates
- **Password Reset**: Secure time-limited reset tokens
- **Rate Limiting**: Protection against brute-force attacks using Bucket4j
- **Healthcare Provider Verification**: License and specialty validation

### 📊 Monitoring & Health
- Spring Boot Actuator integration
- Prometheus metrics export
- Health check endpoints (liveness/readiness probes)
- Structured logging with Logstash

## 🏗️ Architecture

### Technology Stack

| Category | Technology | Version |
|----------|------------|---------|
| **Language** | Java | 21 |
| **Framework** | Spring Boot | 3.4.4 |
| **Security** | Spring Security | 6.x |
| **JWT** | JJWT | 0.13.0 |
| **Database** | PostgreSQL | 16+ |
| **ORM** | Spring Data JPA / Hibernate | Latest |
| **Caching** | Caffeine | Latest |
| **AI** | Google Gemini API | 1.5-flash |
| **Maps** | Google Maps Services | 2.2.0 |
| **API Docs** | SpringDoc OpenAPI | 2.7.0 |
| **Testing** | JUnit 5 + Testcontainers | 1.20.4 |
| **Build** | Maven | 3.6+ |
| **Monitoring** | Micrometer + Prometheus | Latest |

### Project Structure

```
src/main/java/com/medassist/
├── MedassistApplication.java          # Main application entry point
├── ai/                                 # AI Medicine Recognition Module
│   ├── controller/
│   │   └── MedicineAIController.java   # AI analysis endpoints
│   ├── dto/
│   └── service/
│       ├── GeminiAIService.java        # Gemini AI integration
│       ├── MedicineAIService.java      # Medicine analysis logic
│       ├── ImageProcessingService.java # Image handling
│       └── PromptTemplateService.java  # AI prompt management
├── auth/                               # Authentication Module
│   ├── controller/
│   │   ├── AuthenticationController.java
│   │   ├── UserProfileController.java
│   │   ├── HealthCheckController.java
│   │   └── JwtHealthController.java
│   ├── dto/
│   ├── entity/
│   │   ├── User.java
│   │   ├── Role.java
│   │   ├── Permission.java
│   │   ├── VerificationToken.java
│   │   └── PasswordResetToken.java
│   ├── repository/
│   ├── service/
│   └── integration/
├── medicine/                           # Medicine Management Module
│   ├── controller/
│   │   └── MedicineController.java
│   ├── dto/
│   ├── entity/
│   │   └── Medicine.java
│   ├── mapper/
│   ├── repository/
│   └── service/
├── pharmacy/                           # Pharmacy Location Module
│   ├── controller/
│   │   ├── PharmacyController.java
│   │   └── PharmacyLocationController.java
│   ├── dto/
│   ├── entity/
│   │   └── Pharmacy.java
│   ├── mapper/
│   ├── repository/
│   └── service/
└── common/                             # Shared Components
    ├── config/                         # Application configuration
    ├── dto/                            # Common DTOs
    ├── exception/                      # Custom exceptions
    ├── health/                         # Health indicators
    ├── interceptor/                    # Request interceptors
    ├── security/                       # Security configuration
    └── validation/                     # Custom validators
```

### Database Schema

```sql
-- Core entities (auto-created by Hibernate)
users (id, username, email, password_hash, first_name, last_name, phone_number, 
       is_verified, is_enabled, is_healthcare_provider, provider_verified,
       license_number, medical_specialty, hospital_affiliation, created_at, updated_at)

roles (id, name, description)
permissions (id, name, description)
user_roles (user_id, role_id)
role_permissions (role_id, permission_id)
verification_tokens (id, token, user_id, expires_at, verified_at, created_at)
password_reset_tokens (id, token, user_id, expires_at, used_at, created_at)

medicines (id, name, generic_name, manufacturer, category, form, ...)
pharmacies (id, name, address, city, state, zip_code, latitude, longitude, ...)
```

## 🚀 Quick Start

### Prerequisites
- Java 21 or higher
- PostgreSQL 16 or higher
- Maven 3.6 or higher
- Google Gemini API Key
- Google Maps API Key (optional, for pharmacy location)

### 1. Database Setup

```bash
# Create PostgreSQL database
psql -U postgres -f src/main/resources/db/setup.sql

# Or manually:
psql -U postgres -c "CREATE DATABASE medassist_backend_local;"
```

### 2. Environment Configuration

Create a `.env` file in the project root (see `.env.example`):

```bash
# Required
export DATABASE_URL=jdbc:postgresql://localhost:5432/medassist
export DB_USERNAME=your_db_username
export DB_PASSWORD=your_db_password
export JWT_SECRET=your-256-bit-secret-key  # Min 32 characters
export GEMINI_API_KEY=your_gemini_api_key

# Optional
export GOOGLE_MAPS_API_KEY=your_google_maps_api_key
export MAIL_USERNAME=your_email
export MAIL_PASSWORD=your_email_app_password
```

> ⚠️ **Security Note**: Never commit `.env` files or API keys to version control.

### 3. Build and Run

```bash
# Navigate to project directory
cd medassist-backend

# Build the project
./mvnw clean compile

# Run the application
./mvnw spring-boot:run

# Or build and run JAR
./mvnw clean package -DskipTests
java -jar target/medassist-backend-0.0.1-SNAPSHOT.jar
```

The API will be available at `http://localhost:8080`

### 4. Access API Documentation

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

## 🐳 Docker Deployment

### Using Docker Compose (Recommended)

```bash
# Set environment variables
export GEMINI_API_KEY=your_api_key
export JWT_SECRET=your-256-bit-secret-key-here-make-it-long-enough

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f backend

# Stop services
docker-compose down
```

### Using Dockerfile

```bash
# Build the image
docker build -t medassist-backend .

# Run the container
docker run -d \
  -p 8080:8080 \
  -e JWT_SECRET=your-secret-key \
  -e GEMINI_API_KEY=your-api-key \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/medassist \
  medassist-backend
```

## 📚 API Documentation

### Base URL
```
http://localhost:8080/api/v1
```

### Authentication Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/register` | Register new user |
| POST | `/auth/register-healthcare-provider` | Register healthcare provider |
| POST | `/auth/login` | User login |
| POST | `/auth/refresh-token` | Refresh access token |
| POST | `/auth/logout` | User logout |
| GET | `/auth/verify-email` | Verify email token |
| POST | `/auth/forgot-password` | Request password reset |
| POST | `/auth/reset-password` | Reset password |
| GET | `/auth/me` | Get current user info |

### AI Medicine Analysis Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/ai/medicine/analyze/text` | Analyze medicine by text |
| POST | `/ai/medicine/analyze/image` | Analyze medicine by image |
| POST | `/ai/medicine/analyze/combined` | Combined text + image analysis |

### Medicine Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/medicines` | Get all medicines |
| GET | `/medicines/{id}` | Get medicine by ID |
| POST | `/medicines` | Create medicine |
| PUT | `/medicines/{id}` | Update medicine |
| DELETE | `/medicines/{id}` | Delete medicine |
| GET | `/medicines/search` | Comprehensive search |
| GET | `/medicines/search/name` | Search by name |
| GET | `/medicines/search/category` | Search by category |
| GET | `/medicines/search/manufacturer` | Search by manufacturer |

### Pharmacy Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/pharmacies` | Get all pharmacies |
| GET | `/pharmacies/{id}` | Get pharmacy by ID |
| POST | `/pharmacies` | Create pharmacy |
| PUT | `/pharmacies/{id}` | Update pharmacy |
| DELETE | `/pharmacies/{id}` | Delete pharmacy |
| GET | `/pharmacies/search` | Comprehensive search |
| GET | `/pharmacies/nearby` | Find nearby pharmacies |
| POST | `/pharmacies/location/nearby` | Advanced location search |
| GET | `/pharmacies/24hours` | 24-hour pharmacies |
| GET | `/pharmacies/delivery` | Pharmacies with delivery |

### Example Requests

#### Register User
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+1234567890"
  }'
```

#### Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "john_doe",
    "password": "SecurePass123!"
  }'
```

#### Analyze Medicine by Text
```bash
curl -X POST "http://localhost:8080/api/v1/ai/medicine/analyze/text?query=Aspirin%20500mg" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### Analyze Medicine by Image
```bash
curl -X POST http://localhost:8080/api/v1/ai/medicine/analyze/image \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -F "file=@medicine_image.jpg"
```

#### Find Nearby Pharmacies
```bash
curl -X POST http://localhost:8080/api/v1/pharmacies/location/nearby \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "latitude": 37.7749,
    "longitude": -122.4194,
    "radiusKm": 5
  }'
```

## 🔐 Security Configuration

### Password Requirements
- Minimum 8 characters
- At least one uppercase letter (A-Z)
- At least one lowercase letter (a-z)
- At least one number (0-9)
- At least one special character (@$!%*?&)

### JWT Token Configuration
| Token Type | Validity | Purpose |
|------------|----------|---------|
| Access Token | 24 hours | API authentication |
| Refresh Token | 7 days | Obtain new access tokens |

### Rate Limiting
| Operation | Limit | Window |
|-----------|-------|--------|
| Login | 5 attempts | 15 minutes |
| Registration | 3 attempts | 1 hour |
| Password Reset | 3 attempts | 1 hour |

### User Roles & Permissions
| Role | Permissions |
|------|-------------|
| USER | Basic application access |
| HEALTHCARE_PROVIDER | Healthcare features access |
| VERIFIED_HEALTHCARE_PROVIDER | Full medical features |
| ADMIN | System administration |

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=MedicineServiceTest

# Run with coverage report
./mvnw clean test jacoco:report
```

## 📊 Monitoring & Health

### Health Check Endpoints
```bash
# Application health
curl http://localhost:8080/actuator/health

# Liveness probe
curl http://localhost:8080/actuator/health/liveness

# Readiness probe
curl http://localhost:8080/actuator/health/readiness

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

## 🔧 Configuration Reference

### Environment Variables

| Variable | Description | Required |
|----------|-------------|:--------:|
| `DATABASE_URL` | PostgreSQL connection URL | ✅ |
| `DB_USERNAME` | Database username | ✅ |
| `DB_PASSWORD` | Database password | ✅ |
| `JWT_SECRET` | JWT signing key (min 32 characters) | ✅ |
| `GEMINI_API_KEY` | Google Gemini API key for AI features | ✅ |
| `GOOGLE_MAPS_API_KEY` | Google Maps API key for pharmacy location | ❌ |
| `MAIL_USERNAME` | SMTP email username | ❌ |
| `MAIL_PASSWORD` | SMTP email password (app password) | ❌ |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | ❌ |
| `SPRING_PROFILES_ACTIVE` | Spring profile (dev/prod) | ❌ |

## 📈 Changelog

See [CHANGELOG.md](CHANGELOG.md) for version history and updates.

### Latest: v2.0.0 (December 2025)
- ✅ Comprehensive 5-phase security audit
- ✅ JWT token validation hardening
- ✅ Custom exception hierarchy
- ✅ Constructor-based dependency injection
- ✅ Database indexing optimizations
- ✅ Production-ready Docker deployment
- ✅ 36 tests passing

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**Yashvvvv**

- GitHub: [@Yashvvvv](https://github.com/Yashvvvv)

## 🆘 Support

For issues and questions:
- 📖 Check the [API Documentation](http://localhost:8080/swagger-ui.html) (when running locally)
- 🐛 [Open an Issue](https://github.com/Yashvvvv/PharmaLens-AI-Pharmaceutical-Intelligence-Platform/issues) for bug reports
- 💡 [Start a Discussion](https://github.com/Yashvvvv/PharmaLens-AI-Pharmaceutical-Intelligence-Platform/discussions) for feature requests
- 📜 Review the [CHANGELOG](CHANGELOG.md) for version history

## ⭐ Show Your Support

Give a ⭐️ if this project helped you!

---

Built with ❤️ for the PharmaLens AI Pharmaceutical Intelligence Platform.
