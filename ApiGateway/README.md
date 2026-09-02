# API Gateway (Port 8089)

## Overview
The **Spring Cloud API Gateway** acts as the single edge entry point for all frontend client traffic. It performs edge JWT signature verification once and routes requests dynamically to downstream services registered on Netflix Eureka.

## Routing Table
- `/auth/**` &rarr; `lb://UserService`
- `/rider/onboard`, `/rider/getMyProfile`, `/rider/rateDriver/**` &rarr; `lb://UserService`
- `/driver/onboard`, `/driver/getMyProfile`, `/driver/rateRider/**` &rarr; `lb://UserService`
- `/rider/requestRide`, `/rider/cancelRide/**`, `/rider/getMyRides`, `/rider/activeRide`, `/rider/completeRide/**` &rarr; `lb://RideService`
- `/driver/acceptRide/**`, `/driver/startRide/**`, `/driver/endRide/**`, `/driver/cancelRide/**`, `/driver/getMyRides`, `/driver/pendingRides`, `/driver/activeRide`, `/driver/completeRide/**` &rarr; `lb://RideService`
- `/wallet/**`, `/webhooks/razorpay` &rarr; `lb://WalletService`
