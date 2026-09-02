# RideMesh - Phase 2

## Project Overview
**RideMesh** is an event-driven, distributed cab-booking platform decomposed into independent Spring Boot microservices, communicating via **Apache Kafka** event streaming and REST APIs, coordinated by **Netflix Eureka** service discovery and protected by a central **Spring Cloud API Gateway**.

## Team Members
**A042 - Ritish Kumar - 70612500068** <br />
**A064 - Yog Vasaikar - 70612500073** <br />
**A067 - Sumukh Kanekar - 70612500076** <br />

---

## 1. Microservices Breakdown

| Service Name | Port | Database | Primary Responsibility |
|---|---|---|---|
| **eureka-server** | `8761` | N/A | Central Service Registry & Discovery |
| **ApiGateway** | `8089` | N/A | Edge routing, JWT validation & downstream header propagation |
| **UserService** | `8081` | `ridemesh_user_db` (PostgreSQL) | Authentication, JWT issuing, Rider/Driver profiles & onboarding |
| **RideService** | `8082` | `ridemesh_ride_db` (PostgreSQL) | Ride requests, matching algorithm, OTP verification, ride lifecycle |
| **WalletService** | `8083` | `ridemesh_wallet_db` (PostgreSQL) | Rider/Driver balances, Razorpay payment gateway integration, transaction ledgers |
| **NotifyService** | `8084` | N/A | Asynchronous Kafka consumer for email and notification alerts |
| **ThymeleafUI** | `8085` | N/A | Responsive user portal (Rider dashboard, Driver terminal, Wallet) |

---

## 2. Event-Driven Communication via Apache Kafka

![RideMesh Microservices Architecture & Event Flow](https://res.cloudinary.com/dvb5mesnd/image/upload/v1788312748/Screenshot_2026-09-02_070034_jfolav.png)


---

## 3. Database Setup (PostgreSQL)

To set up the distinct databases, execute the following SQL commands in pgAdmin or psql:

```sql
CREATE DATABASE ridemesh_user_db;
CREATE DATABASE ridemesh_ride_db;
CREATE DATABASE ridemesh_wallet_db;
```

---

## 4. Swagger / OpenAPI Documentation

Each microservice exposes interactive Swagger UI documentation for testing and verification:

- **UserService Swagger UI**: [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)
- **RideService Swagger UI**: [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html)
- **WalletService Swagger UI**: [http://localhost:8083/swagger-ui/index.html](http://localhost:8083/swagger-ui/index.html)

---

## 5. API Endpoints Reference

### 🔐 Auth APIs 
| Endpoint | Method | Description |
|---|---|---|
| `/auth/signup` | POST | Register a new user |
| `/auth/login` | POST | Authenticate user and receive JWT tokens |
| `/auth/refresh` | POST | Refresh an expired access token |
| `/auth/onboardDriver/{userId}` | POST | Onboard registered user as a verified driver |

### 🚖 Driver APIs 
| Endpoint | Method | Description |
|---|---|---|
| `/driver/getMyProfile` | GET | Retrieve authenticated driver's profile |
| `/driver/getMyRides` | GET | View driver's trip history |
| `/driver/pendingRides` | GET | List available incoming ride requests |
| `/driver/acceptRide/{rideRequestId}` | POST | Accept a pending ride request |
| `/driver/startRide/{rideId}` | POST | Start trip using rider's 4-digit OTP |
| `/driver/endRide/{rideId}` | POST | Complete ride and trigger wallet payout |
| `/driver/cancelRide/{rideId}` | POST | Cancel an active ride |
| `/driver/rateRider/{rideId}` | POST | Rate rider after trip |

### 👤 Rider APIs 
| Endpoint | Method | Description |
|---|---|---|
| `/rider/getMyProfile` | GET | Retrieve authenticated rider's profile |
| `/rider/getMyRides` | GET | View rider's trip history |
| `/rider/requestRide` | POST | Create a new cab booking request |
| `/rider/cancelRide/{rideId}` | POST | Cancel a pending/active ride request |
| `/rider/rateDriver/{rideId}` | POST | Rate driver after trip |

### 💳 Wallet APIs 
| Endpoint | Method | Description |
|---|---|---|
| `/wallet/balance` | GET | Check current wallet balance |
| `/wallet/create-order` | POST | Initiate Razorpay deposit order |
| `/webhooks/razorpay` | POST | Razorpay webhook listener for instant credit |

---

## 6. How to Run the Platform

### Step 1: Start Infrastructure
Ensure **PostgreSQL** (`localhost:5432`) and **Apache Kafka** (`localhost:9092`) are running.

### Step 2: Start Microservices (in recommended order)
```bash
# 1. Service Discovery
cd eureka-server && ..\mvnw.cmd spring-boot:run

# 2. API Gateway
cd ApiGateway && ..\mvnw.cmd spring-boot:run

# 3. Core Services
cd UserService && ..\mvnw.cmd spring-boot:run
cd RideService && ..\mvnw.cmd spring-boot:run
cd WalletService && ..\mvnw.cmd spring-boot:run
cd NotifyService && ..\mvnw.cmd spring-boot:run

# 4. User Interface
cd ThymeleafUI && ..\mvnw.cmd spring-boot:run
```

### Step 3: Access Application
- **RideMesh App**: [http://localhost:8085](http://localhost:8085)
- **Eureka Registry Dashboard**: [http://localhost:8761](http://localhost:8761)
