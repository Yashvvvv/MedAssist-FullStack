# MedAssist Full-Stack Setup Guide

## Current Status
✅ Java 21 detected  
❌ PostgreSQL not running  
✅ Port 8080 available  

## Prerequisites

### 1. Install PostgreSQL
```bash
# Windows (using winget)
winget install PostgreSQL.PostgreSQL

# Or download from: https://www.postgresql.org/download/windows/
```

### 2. Setup Database
1. Start PostgreSQL service
2. Open pgAdmin or command line
3. Run the provided `setup-database.sql` script:
```sql
psql -U postgres -f setup-database.sql
```

## Running the Application

### Option 1: Backend First (Recommended)

#### Step 1: Start the Backend
```bash
cd medassist-backend/medassist-backend
./mvnw spring-boot:run
```

**Wait for this message:**
```
Started MedassistBackendApplication in X.XXX seconds
```

#### Step 2: Verify Backend
Open browser and go to:
- Health Check: http://localhost:8080/actuator/health
- API Documentation: http://localhost:8080/swagger-ui.html

#### Step 3: Start Android App
1. Open `medassist-android` in Android Studio
2. Start an Android emulator (API 24+)
3. Run the app

### Option 2: Quick Test (Backend Only)

If you want to test backend independently:
```bash
cd medassist-backend/medassist-backend
./mvnw spring-boot:run
```

Test endpoints:
```bash
# Health check
curl http://localhost:8080/actuator/health

# Test authentication endpoint
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@example.com","password":"password123"}'
```

## Network Configuration

### For Android Emulator:
- Backend URL: `http://10.0.2.2:8080/`
- This maps to `localhost:8080` on your host machine

### For Physical Android Device:
- Find your computer's IP address: `ipconfig`
- Update NetworkModule.kt: `http://YOUR_IP:8080/`
- Make sure Windows Firewall allows port 8080

## Troubleshooting

### Backend Issues:

1. **Port 8080 already in use:**
```bash
netstat -ano | findstr :8080
taskkill /PID <PID_NUMBER> /F
```

2. **Database connection failed:**
- Check if PostgreSQL is running: `services.msc` → PostgreSQL
- Verify database exists: `psql -U postgres -l`

3. **Out of memory:**
```bash
set JAVA_OPTS=-Xmx2g
./mvnw spring-boot:run
```

### Android Issues:

1. **Network connection failed:**
- Use `http://10.0.2.2:8080/` for emulator
- Use `http://YOUR_IP:8080/` for physical device
- Check Windows Firewall settings

2. **Build errors:**
```bash
cd medassist-android
./gradlew clean build
```

## Testing Communication

### 1. Backend Health Check
```bash
curl http://localhost:8080/actuator/health
```
Expected response:
```json
{"status":"UP"}
```

### 2. Android Log Check
In Android Studio, check Logcat for:
```
D/OkHttp: --> POST http://10.0.2.2:8080/auth/login
```

### 3. Backend Log Check
Backend console should show:
```
INFO  c.m.m.controller.AuthenticationController : Login attempt for user: test
```

## API Endpoints

### Authentication:
- POST `/auth/register` - User registration
- POST `/auth/login` - User login
- POST `/auth/refresh` - Token refresh
- POST `/auth/logout` - User logout

### Medicine:
- GET `/medicines/search` - Search medicines
- POST `/medicines/analyze` - AI medicine analysis
- GET `/medicines/{id}` - Get medicine details

### Pharmacy:
- GET `/pharmacies/nearby` - Find nearby pharmacies
- GET `/pharmacies/{id}/availability` - Check medicine availability

## Development Tips

1. **Hot Reload Backend:**
   - Backend auto-restarts on file changes (Spring Boot DevTools)

2. **Debug Network Issues:**
   - Enable OkHttp logging in NetworkModule.kt
   - Check backend logs for incoming requests

3. **Database Changes:**
   - Backend creates tables automatically (ddl-auto=update)
   - For fresh start: drop database and recreate

## Next Steps

1. ✅ Fix configuration conflicts
2. ⏳ Start PostgreSQL
3. ⏳ Run backend
4. ⏳ Test Android app
5. ⏳ Clean up redundant UI structure

Run `./test-connectivity.ps1` again after setting up PostgreSQL!
