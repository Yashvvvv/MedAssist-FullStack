# MedAssist Backend API Context Document

## Overview
MedAssist is a comprehensive medical assistance platform with a Spring Boot backend featuring JWT authentication, medicine recognition via AI, pharmacy location services, and healthcare provider management.

**Base URL**: `http://localhost:8080`  
**Technology Stack**: Spring Boot 3.5.3, Java 21, PostgreSQL, JWT, Gemini AI, Google Maps API

---

## 🔐 Authentication & Security

### JWT Token Authentication
- **Access Token**: Expires in 24 hours (86400000 ms)
- **Refresh Token**: Expires in 7 days (604800000 ms)
- **Header Format**: `Authorization: Bearer <access_token>`

### User Roles & Permissions
- **USER**: Basic medical app access
- **HEALTHCARE_PROVIDER**: Extended medical features
- **VERIFIED_HEALTHCARE_PROVIDER**: Full medical access after verification
- **ADMIN**: Complete system access

### Rate Limiting
- **Login**: 5 attempts per 15 minutes
- **Registration**: 3 attempts per hour
- **Password Reset**: 3 attempts per hour

---

## 📱 API Endpoints

### Authentication Endpoints (`/api/auth`)

#### 1. User Registration
**POST** `/api/auth/register`
```json
{
  "username": "string (3-50 chars, required)",
  "email": "string (valid email, required)",
  "password": "string (min 8 chars, 1 upper, 1 lower, 1 digit, 1 special, required)",
  "firstName": "string (max 50 chars, required)",
  "lastName": "string (max 50 chars, required)",
  "phoneNumber": "string (optional, format: +1234567890)"
}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "User registered successfully. Please check your email for verification.",
  "data": {
    "userId": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "emailVerificationRequired": true
  }
}
```

#### 2. Healthcare Provider Registration
**POST** `/api/auth/register-healthcare-provider`
```json
{
  "username": "string (required)",
  "email": "string (required)",
  "password": "string (required)",
  "firstName": "string (required)",
  "lastName": "string (required)",
  "phoneNumber": "string (optional)",
  "licenseNumber": "string (required)",
  "medicalSpecialty": "string (required)",
  "hospitalAffiliation": "string (required)"
}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "Healthcare provider registered successfully. Please check your email for verification and await provider verification.",
  "data": {
    "userId": 1,
    "username": "dr_smith",
    "email": "dr.smith@hospital.com",
    "licenseNumber": "MD123456",
    "emailVerificationRequired": true,
    "providerVerificationRequired": true
  }
}
```

#### 3. User Login
**POST** `/api/auth/login`
```json
{
  "usernameOrEmail": "string (required)",
  "password": "string (required)"
}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "userInfo": {
      "id": 1,
      "username": "john_doe",
      "email": "john@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "phoneNumber": "+1234567890",
      "isVerified": true,
      "isHealthcareProvider": false,
      "providerVerified": false,
      "medicalSpecialty": null,
      "hospitalAffiliation": null,
      "lastLogin": "2025-01-15T10:30:00",
      "roles": ["USER"],
      "permissions": ["READ_MEDICINES", "SEARCH_PHARMACIES"]
    }
  }
}
```

#### 4. Token Refresh
**POST** `/api/auth/refresh-token`
```json
{
  "refreshToken": "string (required)"
}
```

**Response (200 OK)**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer"
}
```

#### 5. Email Verification
**GET** `/api/auth/verify-email?token=<verification_token>`

**Response (200 OK)**:
```json
{
  "message": "Email verified successfully"
}
```

#### 6. Forgot Password
**POST** `/api/auth/forgot-password`
```json
{
  "email": "string (required)"
}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "Password reset email sent successfully",
  "data": null
}
```

#### 7. Reset Password
**POST** `/api/auth/reset-password`
```json
{
  "token": "string (required)",
  "newPassword": "string (required, same validation as registration)"
}
```

#### 8. Change Password
**POST** `/api/auth/change-password`
**Headers**: `Authorization: Bearer <access_token>`
```json
{
  "currentPassword": "string (required)",
  "newPassword": "string (required)"
}
```

#### 9. Get Current User
**GET** `/api/auth/me`
**Headers**: `Authorization: Bearer <access_token>`

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "User information retrieved successfully",
  "data": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+1234567890",
    "isVerified": true,
    "isHealthcareProvider": false,
    "providerVerified": false,
    "medicalSpecialty": null,
    "hospitalAffiliation": null,
    "lastLogin": "2025-01-15T10:30:00",
    "roles": ["USER"],
    "permissions": ["READ_MEDICINES", "SEARCH_PHARMACIES"]
  }
}
```

#### 10. Logout
**POST** `/api/auth/logout`
**Headers**: `Authorization: Bearer <access_token>`

#### 11. Resend Verification Email
**POST** `/api/auth/resend-verification?email=<email>`

---

### User Profile Endpoints (`/api/profile`)

#### 1. Get Current User Profile
**GET** `/api/profile/me`
**Headers**: `Authorization: Bearer <access_token>`

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "Profile retrieved successfully",
  "data": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+1234567890",
    "isVerified": true,
    "isHealthcareProvider": false,
    "providerVerified": false,
    "medicalSpecialty": "",
    "hospitalAffiliation": "",
    "licenseNumber": "",
    "lastLogin": "2025-01-15T10:30:00",
    "createdAt": "2025-01-10T09:00:00"
  }
}
```

#### 2. Update User Profile
**PUT** `/api/profile/update`
**Headers**: `Authorization: Bearer <access_token>`
```json
{
  "firstName": "string (optional)",
  "lastName": "string (optional)",
  "phoneNumber": "string (optional)",
  "medicalSpecialty": "string (optional, for healthcare providers)",
  "hospitalAffiliation": "string (optional, for healthcare providers)"
}
```

#### 3. Deactivate Account
**POST** `/api/profile/deactivate`
**Headers**: `Authorization: Bearer <access_token>`

#### 4. Get Account Status
**GET** `/api/profile/account-status`
**Headers**: `Authorization: Bearer <access_token>`

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "Account status retrieved successfully",
  "data": {
    "isActive": true,
    "isVerified": true,
    "isHealthcareProvider": false,
    "providerVerified": false,
    "accountCreated": "2025-01-10T09:00:00",
    "lastLogin": "2025-01-15T10:30:00"
  }
}
```

---

### Medicine Endpoints (`/api/medicines`)

#### 1. Get All Medicines
**GET** `/api/medicines`
**Headers**: `Authorization: Bearer <access_token>`

**Response (200 OK)**:
```json
[
  {
    "id": 1,
    "name": "Paracetamol",
    "genericName": "Acetaminophen",
    "brandNames": ["Tylenol", "Panadol"],
    "description": "Pain reliever and fever reducer",
    "usageDescription": "For headaches, muscle aches, arthritis, backaches, toothaches, colds, and fevers",
    "dosageInformation": "Adults: 500mg-1000mg every 4-6 hours. Maximum 4000mg per day",
    "sideEffects": ["Nausea", "Stomach upset", "Allergic reactions"],
    "manufacturer": "GSK",
    "category": "Analgesic",
    "form": "Tablet",
    "strength": "500mg",
    "activeIngredients": ["Acetaminophen 500mg"],
    "requiresPrescription": false,
    "isActive": true,
    "createdAt": "2025-01-10T09:00:00",
    "updatedAt": "2025-01-10T09:00:00"
  }
]
```

#### 2. Get Medicine by ID
**GET** `/api/medicines/{id}`
**Headers**: `Authorization: Bearer <access_token>`

#### 3. Create Medicine (Admin Only)
**POST** `/api/medicines`
**Headers**: `Authorization: Bearer <access_token>`
```json
{
  "name": "string (required)",
  "genericName": "string (required)",
  "brandNames": ["string"],
  "description": "string (optional)",
  "usageDescription": "string (optional)",
  "dosageInformation": "string (optional)",
  "sideEffects": ["string"],
  "manufacturer": "string (required)",
  "category": "string (required)",
  "form": "string (required)",
  "strength": "string (required)",
  "activeIngredients": ["string"],
  "requiresPrescription": "boolean (required)",
  "isActive": "boolean (default: true)"
}
```

#### 4. Update Medicine (Admin Only)
**PUT** `/api/medicines/{id}`
**Headers**: `Authorization: Bearer <access_token>`

#### 5. Delete Medicine (Admin Only)
**DELETE** `/api/medicines/{id}`
**Headers**: `Authorization: Bearer <access_token>`

#### 6. Search Medicines
**GET** `/api/medicines/search?q=<search_term>`
**Headers**: `Authorization: Bearer <access_token>`

#### 7. Search by Specific Fields
- **GET** `/api/medicines/search/name?name=<name>`
- **GET** `/api/medicines/search/generic?genericName=<generic_name>`
- **GET** `/api/medicines/search/manufacturer?manufacturer=<manufacturer>`
- **GET** `/api/medicines/search/category?category=<category>`
- **GET** `/api/medicines/search/form?form=<form>`
- **GET** `/api/medicines/search/brand?brandName=<brand_name>`
- **GET** `/api/medicines/search/ingredient?ingredient=<ingredient>`
- **GET** `/api/medicines/search/prescription?requiresPrescription=<boolean>`
- **GET** `/api/medicines/search/strength?strength=<strength>`

---

### AI Medicine Recognition Endpoints (`/api/ai/medicine`)

#### 1. Analyze Medicine by Text
**POST** `/api/ai/medicine/analyze/text`
**Headers**: `Authorization: Bearer <access_token>`
```json
{
  "query": "string (required) - describe the medicine or symptoms"
}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "analysisType": "TEXT_ANALYSIS",
  "query": "headache medicine",
  "medicines": [
    {
      "id": 1,
      "name": "Paracetamol",
      "genericName": "Acetaminophen",
      "confidence": 0.95,
      "matchReason": "Commonly used for headaches"
    }
  ],
  "aiInsights": {
    "summary": "Based on your query about headache medicine, here are the most suitable options",
    "recommendations": [
      "Take as directed on package",
      "Consult doctor if symptoms persist"
    ],
    "warnings": [
      "Do not exceed recommended dosage",
      "Avoid alcohol while taking medication"
    ]
  },
  "timestamp": "2025-01-15T10:30:00"
}
```

#### 2. Analyze Medicine by Image
**POST** `/api/ai/medicine/analyze/image`
**Headers**: `Authorization: Bearer <access_token>`
**Content-Type**: `multipart/form-data`

**Form Data**:
- `image`: File (required, max 10MB, JPG/PNG)

**Response (200 OK)**:
```json
{
  "success": true,
  "analysisType": "IMAGE_ANALYSIS",
  "imageAnalysis": {
    "detectedText": "PARACETAMOL 500mg",
    "confidence": 0.92,
    "imageQuality": "HIGH"
  },
  "medicines": [
    {
      "id": 1,
      "name": "Paracetamol",
      "genericName": "Acetaminophen",
      "confidence": 0.92,
      "matchReason": "Text detected on medicine package"
    }
  ],
  "aiInsights": {
    "summary": "Medicine identified from image analysis",
    "recommendations": [
      "Verify medicine details before consumption",
      "Check expiration date on package"
    ],
    "warnings": [
      "Ensure medicine is not damaged",
      "Store in cool, dry place"
    ]
  },
  "timestamp": "2025-01-15T10:30:00"
}
```

#### 3. Combined Analysis (Text + Image)
**POST** `/api/ai/medicine/analyze/combined`
**Headers**: `Authorization: Bearer <access_token>`
**Content-Type**: `multipart/form-data`

**Form Data**:
- `image`: File (required)
- `query`: String (required)

---

### Pharmacy Endpoints (`/api/pharmacies`)

#### 1. Get All Pharmacies
**GET** `/api/pharmacies`
**Headers**: `Authorization: Bearer <access_token>`

**Response (200 OK)**:
```json
[
  {
    "id": 1,
    "name": "City Pharmacy",
    "address": "123 Main St, City, State 12345",
    "phoneNumber": "+1234567890",
    "email": "info@citypharmacy.com",
    "website": "https://citypharmacy.com",
    "operatingHours": {
      "monday": "08:00-20:00",
      "tuesday": "08:00-20:00",
      "wednesday": "08:00-20:00",
      "thursday": "08:00-20:00",
      "friday": "08:00-20:00",
      "saturday": "09:00-18:00",
      "sunday": "10:00-16:00"
    },
    "services": ["Prescription Filling", "Consultation", "Home Delivery"],
    "isActive": true,
    "latitude": 40.7128,
    "longitude": -74.0060,
    "createdAt": "2025-01-10T09:00:00",
    "updatedAt": "2025-01-10T09:00:00"
  }
]
```

#### 2. Get Pharmacy by ID
**GET** `/api/pharmacies/{id}`
**Headers**: `Authorization: Bearer <access_token>`

#### 3. Create Pharmacy (Admin Only)
**POST** `/api/pharmacies`
**Headers**: `Authorization: Bearer <access_token>`
```json
{
  "name": "string (required)",
  "address": "string (required)",
  "phoneNumber": "string (required)",
  "email": "string (optional)",
  "website": "string (optional)",
  "operatingHours": {
    "monday": "string (HH:MM-HH:MM)",
    "tuesday": "string",
    "wednesday": "string",
    "thursday": "string",
    "friday": "string",
    "saturday": "string",
    "sunday": "string"
  },
  "services": ["string"],
  "latitude": "number (required)",
  "longitude": "number (required)",
  "isActive": "boolean (default: true)"
}
```

#### 4. Update Pharmacy (Admin Only)
**PUT** `/api/pharmacies/{id}`
**Headers**: `Authorization: Bearer <access_token>`

#### 5. Delete Pharmacy (Admin Only)
**DELETE** `/api/pharmacies/{id}`
**Headers**: `Authorization: Bearer <access_token>`

#### 6. Deactivate Pharmacy (Admin Only)
**PATCH** `/api/pharmacies/{id}/deactivate`
**Headers**: `Authorization: Bearer <access_token>`

#### 7. Search Pharmacies
**GET** `/api/pharmacies/search?q=<search_term>`
**Headers**: `Authorization: Bearer <access_token>`

---

### Pharmacy Location Endpoints (`/api/pharmacies/location`)

#### 1. Find Nearby Pharmacies
**POST** `/api/pharmacies/location/nearby`
**Headers**: `Authorization: Bearer <access_token>`
```json
{
  "latitude": "number (required, -90 to 90)",
  "longitude": "number (required, -180 to 180)",
  "radiusKm": "number (optional, default: 10, max: 50)",
  "maxResults": "number (optional, default: 20, max: 100)",
  "includeOperatingHours": "boolean (optional, default: true)",
  "includeServices": "boolean (optional, default: true)",
  "filterByServices": ["string (optional)"],
  "sortBy": "string (optional: DISTANCE, RATING, NAME, default: DISTANCE)"
}
```

**Response (200 OK)**:
```json
[
  {
    "id": 1,
    "name": "City Pharmacy",
    "address": "123 Main St, City, State 12345",
    "phoneNumber": "+1234567890",
    "email": "info@citypharmacy.com",
    "website": "https://citypharmacy.com",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "distance": 1.2,
    "distanceUnit": "km",
    "operatingHours": {
      "monday": "08:00-20:00",
      "tuesday": "08:00-20:00",
      "wednesday": "08:00-20:00",
      "thursday": "08:00-20:00",
      "friday": "08:00-20:00",
      "saturday": "09:00-18:00",
      "sunday": "10:00-16:00"
    },
    "services": ["Prescription Filling", "Consultation", "Home Delivery"],
    "isCurrentlyOpen": true,
    "nextOpeningTime": null,
    "nextClosingTime": "2025-01-15T20:00:00",
    "googleMapsUrl": "https://maps.google.com/?q=40.7128,-74.0060",
    "directionsUrl": "https://maps.google.com/maps/dir/?api=1&destination=40.7128,-74.0060"
  }
]
```

#### 2. Find Nearby Pharmacies (GET)
**GET** `/api/pharmacies/location/nearby?latitude=<lat>&longitude=<lon>&radiusKm=<radius>`
**Headers**: `Authorization: Bearer <access_token>`

---

### Health Check Endpoint

#### Health Check
**GET** `/api/health`

**Response (200 OK)**:
```json
{
  "status": "UP",
  "timestamp": "2025-01-15T10:30:00",
  "services": {
    "database": "UP",
    "ai": "UP",
    "maps": "UP"
  }
}
```

---

## 📊 Data Models

### User Entity
```json
{
  "id": "number",
  "username": "string (3-50 chars, unique)",
  "email": "string (valid email, unique)",
  "firstName": "string (max 50 chars)",
  "lastName": "string (max 50 chars)",
  "phoneNumber": "string (optional)",
  "isVerified": "boolean",
  "isEnabled": "boolean",
  "createdAt": "datetime",
  "updatedAt": "datetime",
  "lastLogin": "datetime",
  "licenseNumber": "string (optional, for healthcare providers)",
  "medicalSpecialty": "string (optional, for healthcare providers)",
  "hospitalAffiliation": "string (optional, for healthcare providers)",
  "isHealthcareProvider": "boolean",
  "providerVerified": "boolean",
  "roles": ["string"],
  "verificationTokens": ["object"],
  "passwordResetTokens": ["object"]
}
```

### Medicine Entity
```json
{
  "id": "number",
  "name": "string (unique)",
  "genericName": "string",
  "brandNames": ["string"],
  "description": "text",
  "usageDescription": "text",
  "dosageInformation": "text",
  "sideEffects": ["string"],
  "manufacturer": "string",
  "category": "string",
  "form": "string",
  "strength": "string",
  "activeIngredients": ["string"],
  "requiresPrescription": "boolean",
  "isActive": "boolean",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

### Pharmacy Entity
```json
{
  "id": "number",
  "name": "string",
  "address": "string",
  "phoneNumber": "string",
  "email": "string (optional)",
  "website": "string (optional)",
  "operatingHours": "object",
  "services": ["string"],
  "latitude": "number",
  "longitude": "number",
  "isActive": "boolean",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

---

## 🔧 Configuration

### Database Configuration
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/medassist_auth
spring.datasource.username=medassist_user
spring.datasource.password=medassist_password
```

### JWT Configuration
```properties
jwt.secret=medassist-super-secret-key-change-in-production
jwt.expiration=86400000
jwt.refresh-expiration=604800000
```

### External APIs
```properties
gemini.api.key=your_gemini_api_key_here
google.maps.api-key=your_google_maps_api_key_here
```

### Rate Limiting
```properties
rate-limit.login.attempts=5
rate-limit.login.window-minutes=15
rate-limit.registration.attempts=3
rate-limit.registration.window-hours=1
```

---

## 🚨 Error Handling

### Standard Error Response Format
```json
{
  "success": false,
  "error": {
    "message": "string",
    "code": "string",
    "timestamp": "number"
  }
}
```

### Common Error Codes
- `AUTHENTICATION_FAILED`: Invalid credentials
- `TOKEN_EXPIRED`: JWT token expired
- `INVALID_TOKEN`: Invalid JWT token
- `RATE_LIMIT_EXCEEDED`: Too many requests
- `VALIDATION_ERROR`: Request validation failed
- `NOT_FOUND`: Resource not found
- `UNAUTHORIZED`: Insufficient permissions
- `INTERNAL_SERVER_ERROR`: Server error

### HTTP Status Codes
- `200 OK`: Success
- `201 Created`: Resource created
- `400 Bad Request`: Invalid request
- `401 Unauthorized`: Authentication required
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Resource not found
- `409 Conflict`: Resource already exists
- `429 Too Many Requests`: Rate limit exceeded
- `500 Internal Server Error`: Server error

---

## 🔒 Security Features

### Password Requirements
- Minimum 8 characters
- At least 1 uppercase letter
- At least 1 lowercase letter
- At least 1 number
- At least 1 special character (@$!%*?&)

### Security Headers
- CORS enabled for all origins
- JWT token validation on protected endpoints
- Input validation and sanitization
- SQL injection protection
- XSS protection

### Email Verification
- HTML email templates
- Time-limited verification tokens
- Automatic cleanup of expired tokens

---

## 📧 Email Templates

### Verification Email
- Professional HTML design
- Secure verification link
- Clear instructions
- Responsive layout

### Password Reset Email
- Security guidelines
- Time-limited reset link
- Contact information
- Mobile-friendly design

### Healthcare Provider Verification
- Specialized template for healthcare providers
- Additional verification steps
- Professional appearance

---

## 🏥 Healthcare Provider Features

### Registration Process
1. Submit registration with license number
2. Email verification
3. Admin review and verification
4. Provider verification confirmation
5. Access to healthcare features

### Additional Fields
- License number
- Medical specialty
- Hospital affiliation
- Provider verification status

---

## 🤖 AI Integration

### Gemini AI Configuration
- Model: gemini-1.5-flash
- Timeout: 30 seconds
- Max retries: 3
- Image analysis support
- Text analysis support

### Google Maps Integration
- Pharmacy location services
- Distance calculations
- Directions URLs
- Operating hours integration

---

## 📱 Mobile App Integration Tips

### Authentication Flow
1. Register/Login to get tokens
2. Store tokens securely (Android Keystore)
3. Include Bearer token in all API calls
4. Handle token refresh automatically
5. Implement proper logout

### Image Upload
- Use multipart/form-data
- Compress images before upload
- Handle upload progress
- Validate file types and sizes

### Location Services
- Request location permissions
- Get user's current location
- Search nearby pharmacies
- Display results on map

### Error Handling
- Parse error responses consistently
- Show user-friendly error messages
- Handle network errors gracefully
- Implement retry mechanisms

### Offline Support
- Cache frequently accessed data
- Store user profile locally
- Queue API calls when offline
- Sync when connection restored

---

## 🔧 Testing

### Available Test Endpoints
- Unit tests for all services
- Integration tests for API endpoints
- Authentication flow tests
- Database connectivity tests

### Test Database
- Separate test database configuration
- Automated test data setup
- Test data cleanup after tests

---

This comprehensive document provides all the necessary information for developing a beautiful Android frontend for the MedAssist backend. The API follows RESTful conventions, implements proper security measures, and provides detailed error handling to ensure a smooth mobile app development experience.
