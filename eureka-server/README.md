# Eureka Discovery Server (Port 8761)

## Overview
The **Eureka Server** provides centralized microservice registration and dynamic discovery. All microservices automatically register their host and port with Eureka upon startup, allowing the API Gateway and `RestTemplate` clients to resolve endpoints dynamically (`lb://ServiceName`).

## Dashboard
- **Eureka Dashboard**: [http://localhost:8761](http://localhost:8761)
