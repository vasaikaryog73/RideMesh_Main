# RideMesh - Phase 3

## Project Overview
**RideMesh** is an enterprise-grade, event-driven, distributed cab-booking platform decomposed into independent Spring Boot microservices. It features:
- **Asynchronous Event-Driven Architecture** powered by **Apache Kafka** event streaming.
- **Service Discovery & Client-Side Load Balancing** coordinated by **Netflix Eureka**.
- **Edge Routing & JWT Security** managed by **Spring Cloud API Gateway**.
- **Observability & Health Monitoring** using **Spring Boot 3 Actuator**, **Micrometer Prometheus registry**, **Prometheus**, and custom **Grafana** dashboards.
- **Full Containerization & Cloud-Native Orchestration** with optimized multi-stage **Dockerfiles**, **Docker Compose**, and **Kubernetes** Deployment & Service manifests.
- **Interactive Web Portal** built with **Thymeleaf**, responsive Tailwind styling, Leaflet OpenStreetMap geocoding, and Razorpay payment gateway integration.

## Team Members
**A042 - Ritish Kumar - 70612500068** <br />
**A064 - Yog Vasaikar - 70612500073** <br />
**A067 - Sumukh Kanekar - 70612500076** <br />

<img width="1917" height="927" alt="image" src="https://github.com/user-attachments/assets/c37a9483-9d63-495a-bab5-52c2a88a89e2" />


---

## 1. Microservices Breakdown & Ports

| Service Name | Port | Database | Primary Responsibility |
|---|---|---|---|
| **eureka-server** | `8761` | N/A | Central Service Registry & Discovery |
| **ApiGateway** | `8089` | N/A | Edge routing, JWT validation & downstream header propagation |
| **UserService** | `8081` | `ridemesh_user_db` (PostgreSQL) | Authentication, JWT issuing, Rider/Driver profiles & onboarding |
| **RideService** | `8082` | `ridemesh_ride_db` (PostgreSQL) | Ride requests, matching algorithm, OTP verification, ride lifecycle |
| **WalletService** | `8083` | `ridemesh_wallet_db` (PostgreSQL) | Rider/Driver balances, Razorpay payment gateway integration, transaction ledgers |
| **NotifyService** | `8084` | `notify_db` (PostgreSQL) | Asynchronous Kafka consumer for email and notification alerts |
| **ThymeleafUI** | `8085` | N/A | Interactive user portal (Rider dashboard, Driver terminal, Wallet) |
| **Prometheus** | `9090` | N/A | Time-series metrics scraper for microservices Actuator endpoints |
| **Grafana** | `3000` | N/A | Real-time observability dashboard for JVM, throughput, and error rates |

---

## 2. Key Platform Features & Latest Updates

### 📍 Full Address Precision & Interactive History
- **Complete Address Capture**: Captures and persists entire detailed addresses (e.g., `"Cooper Hospital, Gulmohar Road, Vile Parle West, K/W Ward, Mumbai Zone 3, Mumbai, Mumbai Suburban District, Maharashtra, 400057, India"`) for both pickup and dropoff across booking, requests, and ride history tables.
- **Double-Click Inline Copying**: Address cells in Rider History (`/rider/history`) and Driver Completed Rides (`/driver/history`) truncate cleanly in table view, but on **double-click** (`ondblclick`), seamlessly transform into an editable, pre-selected textbox allowing instant `Ctrl+A` and `Ctrl+C` copying of the complete unabbreviated address string.
- **Universal History Search Bars**: Filter ride tables instantly by Ride ID, Driver/Rider name, pickup address, or dropoff address.

### 🛡️ Driver Incident Reporting & Moderation
- **Rider Incident Reports**: Riders can report drivers from their history table or active ride with a compulsory reason and media/photo evidence uploaded via Cloudinary.
- **Automated Acknowledgment**: Instant confirmation email dispatched to rider upon receipt.
- **Admin Report Applications (`/admin/reports`)**: Admin dashboard to review complaints and compulsory evidence, send official notices to drivers, or suspend driver accounts directly.

### 💡 User Feedback, Suggestions, Complaints & Bug Reporting
- **User Profile Modal (`/profile`)**: Accessible to both Riders and Drivers via a modern, fixed-height, scrollable modal:
  - **Feedback**: Dynamic sub-type selection for *Positive Review* or *Negative Review*, with optional media attachment.
  - **Suggestions**: Direct idea submission box with optional mockup/image attachments.
  - **Complaints**: Contextual classification for *Against Driver* or *Against Platform*.
  - **Bug Reports**: Compulsory media attachment (screenshot/screen recording) with submit button dynamically locked/grayed out until evidence is attached.
- **Admin Improvement Console (`/admin/improvement-console`)**:
  - Filter tabs for *All Items*, *Feedback*, *Suggestions*, *Complaints*, and *Bug Reports*.
  - Real-time search across sender name, email, ID, category, and content.
  - Interactive **Email Reply Modal** allowing administrators to dispatch official responses directly to the user's registered email via Kafka notifications.

### 🎚️ Driver Availability Switch & Terminal Auto-Shield
- **Duty Toggle Switch**: Interactive, persistent toggle button on the driver profile (`/profile`) switching between **Available** and **Unavailable**.
- **Matchmaking Exclusion**: Unavailable drivers are immediately blocked from matchmaking, preventing incoming requests on their terminal and directing them to toggle back to Available when ready.

---

## 3. Event-Driven Communication via Apache Kafka

![RideMesh Microservices Architecture & Event Flow](https://res.cloudinary.com/dvb5mesnd/image/upload/v1788312748/Screenshot_2026-09-02_070034_jfolav.png)

Key Kafka Topics:
- `ride.requested`: Published by `RideService` when rider initiates a booking.
- `ride.confirmed`: Published when driver accepts a ride request.
- `ride.started`: Published after OTP verification.
- `ride.completed`: Triggers fare deduction in `WalletService` and driver payout.
- `notification.email`: Consumed by `NotifyService` to dispatch automated SMTP email notifications.

### Kafka Setup

#### 1. Pull latest image from Docker Hub
docker pull apache/kafka:latest

#### 2. Run Kafka container in KRaft mode on port 9092
docker run -d --name kafka -p 9092:9092 apache/kafka:latest


---

## 3. Observability, Metrics & Grafana

Every microservice exposes standardized Actuator endpoints for health checks, Kubernetes probes, and Prometheus metrics:
- `/actuator/health`: System health check (Database, Kafka, Disk Space)
- `/actuator/info`: Service metadata and runtime info
- `/actuator/prometheus`: Micrometer Prometheus metrics feed

### Accessing Dashboards:
- **Prometheus UI**: [http://localhost:9090](http://localhost:9090)
- **Grafana UI**: [http://localhost:3000](http://localhost:3000) (Default login: `admin` / `admin`)
  - Auto-provisioned datasource: **Prometheus**
  - Pre-configured dashboard: **RideMesh - Microservices Overview** (JVM Heap, Process CPU, Request Throughput, Thread count)

---

## 4. Swagger / OpenAPI Documentation

Each microservice exposes interactive Swagger UI documentation for testing and verification:

- **UserService Swagger UI**: [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)
- **RideService Swagger UI**: [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html)
- **WalletService Swagger UI**: [http://localhost:8083/swagger-ui/index.html](http://localhost:8083/swagger-ui/index.html)

---

## 5. Deployment Options

### Option A: Run with Docker Compose (Recommended)
Launch the entire platform including databases, Kafka, all 7 microservices, Prometheus, and Grafana with one command:

```bash
docker-compose up --build -d
```

To view logs:
```bash
docker-compose logs -f
```

To stop:
```bash
docker-compose down
```

### Option B: Deploy to Kubernetes
Kubernetes manifests are located under `k8s/` and include complete Deployments, Services, ConfigMaps, Secrets, PVCs, and Liveness/Readiness probes:

```bash
# 1. Create Namespace
kubectl apply -f k8s/00-namespace.yaml

# 2. ConfigMaps and Secrets
kubectl apply -f k8s/01-configmap-secrets.yaml

# 3. Infrastructure (PostgreSQL & Kafka)
kubectl apply -f k8s/02-postgres.yaml
kubectl apply -f k8s/03-kafka.yaml

# 4. Service Discovery & Gateway
kubectl apply -f k8s/04-eureka.yaml
kubectl apply -f k8s/05-apigateway.yaml

# 5. Core Microservices
kubectl apply -f k8s/06-user-service.yaml
kubectl apply -f k8s/07-ride-service.yaml
kubectl apply -f k8s/08-wallet-service.yaml
kubectl apply -f k8s/09-notify-service.yaml

# 6. UI & Monitoring
kubectl apply -f k8s/10-thymeleaf-ui.yaml
kubectl apply -f k8s/11-monitoring.yaml
```

Accessing the application in Kubernetes:
- **Thymeleaf UI**: `http://<NODE_IP>:30085`
- **Grafana**: `http://<NODE_IP>:30300`
- **Prometheus**: `http://<NODE_IP>:30090`

### Option C: Run Locally via Maven
Ensure PostgreSQL (`localhost:5432`) and Apache Kafka (`localhost:9092`) are running, then start the services in order:

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

---

## 6. Access Points
- **RideMesh App**: [http://localhost:8085](http://localhost:8085)
- **Eureka Registry Dashboard**: [http://localhost:8761](http://localhost:8761)
- **API Gateway**: [http://localhost:8089](http://localhost:8089)
- **Grafana Dashboard**: [http://localhost:3000](http://localhost:3000)
- **Prometheus Server**: [http://localhost:9090](http://localhost:9090)

---

## 7. Application UI images

### Auth Pages :

<img width="1917" height="930" alt="image" src="https://github.com/user-attachments/assets/8164da9d-811a-4b5d-b7ee-15b7287c2e69" /> <br/>

<img width="1917" height="928" alt="image" src="https://github.com/user-attachments/assets/83703d13-53a0-4798-b05b-12bc893fde7e" /> <br/>

<img width="1916" height="932" alt="image" src="https://github.com/user-attachments/assets/7cee5907-cb29-4ac7-a1b7-1d9a5f6b8609" /> <br/>


### Rider Dashboard :

<img width="1916" height="927" alt="image" src="https://github.com/user-attachments/assets/f3afcb5c-9694-4417-be42-ccc590ad6746" /> <br/>

<img width="1917" height="925" alt="image" src="https://github.com/user-attachments/assets/005d42fd-b6c0-4c76-81f5-adaff9ab8f73" /> <br/>

<img width="1917" height="927" alt="image" src="https://github.com/user-attachments/assets/8fd1c895-0e48-4edf-99cc-525998e824a4" /> <br/>

<img width="1917" height="928" alt="image" src="https://github.com/user-attachments/assets/b144f4f6-538d-4b70-af9b-eb8b227f4a7a" /> <br/>

<img width="1917" height="927" alt="image" src="https://github.com/user-attachments/assets/3e4bef2d-f134-4435-9eee-15bf2be3c3d4" /> <br/>

<img width="1917" height="928" alt="image" src="https://github.com/user-attachments/assets/0498ed19-33ec-4f59-b5ba-290b9e61a48b" /> <br/>


### Driver Dashboard :

<img width="1917" height="927" alt="image" src="https://github.com/user-attachments/assets/fe2a9442-9a37-4d2f-a736-57b03d948064" /> <br/>

<img width="1917" height="925" alt="image" src="https://github.com/user-attachments/assets/a17f5850-2cea-4264-aeb7-ea6f8a5fb08e" /> <br/>

<img width="1917" height="927" alt="image" src="https://github.com/user-attachments/assets/594d7a8e-fe6d-4092-9636-885a310053e4" /> <br/>


### Admin Dashboard :

Admin Login Page : http://localhost:8085/admin-login
Admin Email : admin@ridemesh.com
Admin Password : AdminPass123

<img width="1917" height="927" alt="image" src="https://github.com/user-attachments/assets/f0c2f42d-9415-495b-89df-2c1d94c0a226" /> <br/>

<img width="1917" height="927" alt="image" src="https://github.com/user-attachments/assets/a2f331ed-ffd8-4998-9490-7535d72defc6" /> <br/>

<img width="1917" height="925" alt="image" src="https://github.com/user-attachments/assets/578130dd-91d3-456c-acc2-f92d3e152264" /> <br/>
