# User Service (Port 8081)

## Overview
The **User Service** manages user authentication, role-based access control (`ROLE_RIDER`, `ROLE_DRIVER`, `ROLE_ADMIN`), rider and driver profiles, driver onboarding, and ratings.

## Database
- **Engine**: PostgreSQL
- **Database**: `ridemesh_user_db`

## Kafka Topics
- **Consumes**:
  - `ride-accepted-event`: Sets driver `available = false` when accepting a ride.
  - `ride-ended-event`: Resets driver `available = true` when ride is completed.

## API Endpoints & Swagger Documentation
- **Swagger UI**: [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)

### Auth APIs (`auth-post-mapping`)
- `POST /auth/signup` - Register a new user
- `POST /auth/login` - Authenticate user & issue JWT
- `POST /auth/refresh` - Refresh access token
- `POST /auth/onboardDriver/{userId}` - Onboard driver with vehicle details

### Driver Profile APIs (`driver-get-mapping`, `driver-post-mapping`)
- `GET /driver/getMyProfile` - Get authenticated driver profile
- `POST /driver/rateRider/{rideId}` - Rate rider

### Rider Profile APIs (`rider-get-mapping`, `rider-post-mapping`)
- `GET /rider/getMyProfile` - Get authenticated rider profile
- `POST /rider/rateDriver/{rideId}` - Rate driver

### Driver Incident Reporting APIs (`/auth/driver-reports`)
- `POST /auth/driver-reports` - Submit driver report with compulsory evidence media (dispatches email confirmation)
- `GET /auth/driver-reports` - List all reports for Admin review
- `POST /auth/driver-reports/{reportId}/send-notice` - Send official warning/notice to reported driver
- `POST /auth/driver-reports/{reportId}/suspend-driver` - Suspend driver account directly

### Improvement Console & Feedback APIs (`/auth/feedback`)
- `POST /auth/feedback` - Submit user feedback, suggestions, complaints, or bug reports (compulsory media for bugs)
- `GET /auth/feedback` - List feedback items filtered by type for Admin Improvement Console
- `POST /auth/feedback/{id}/reply` - Admin email response to user inquiry via Kafka notification
