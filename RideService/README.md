# Ride Service (Port 8082)

## Overview
The **Ride Service** orchestrates the complete lifecycle of a ride request: fare calculation, driver matching, OTP generation, ride starting, ride completion, cancellation, and ride history.

## Database
- **Engine**: PostgreSQL
- **Database**: `ridemesh_ride_db`

## Kafka Topics
- **Produces**:
  - `ride-created-topic`: Notifies nearby drivers of a new booking request.
  - `ride-accepted-event`: Tells `UserService` to lock the driver's availability.
  - `ride-otp-notification`: Tells `NotifyService` to dispatch an OTP email to the rider.
  - `ride-ended-event`: Tells `WalletService` to process fare settlement and `UserService` to free the driver.

## API Endpoints & Swagger Documentation
- **Swagger UI**: [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html)

### Rider Operations (`rider-post-mapping`, `rider-get-mapping`)
- `POST /rider/requestRide` - Create a new cab booking request
- `POST /rider/cancelRide/{rideId}` - Cancel a ride request
- `GET /rider/getMyRides` - Get rider trip history
- `GET /rider/activeRide` - Get current active ride

### Driver Operations (`driver-post-mapping`, `driver-get-mapping`)
- `GET /driver/pendingRides` - List available incoming requests
- `POST /driver/acceptRide/{rideRequestId}` - Accept a ride request
- `POST /driver/startRide/{rideId}` - Start ride with rider's OTP
- `POST /driver/endRide/{rideId}` - Finish ride and trigger payment
- `POST /driver/cancelRide/{rideId}` - Cancel active ride
- `GET /driver/getMyRides` - Get driver trip history
- `GET /driver/activeRide` - Get current active ride
