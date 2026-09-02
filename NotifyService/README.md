# Notify Service (Port 8084)

## Overview
The **Notify Service** is a dedicated asynchronous notification worker. It has no REST state and acts purely as an event consumer off the Kafka message bus, dispatching emails and real-time alerts.

## Kafka Topics
- **Consumes**:
  - `ride-created-topic`: Sends email alert to matching drivers.
  - `ride-otp-notification`: Sends 4-digit ride OTP verification email to the rider.
  - `payment-completed-event`: Sends invoice receipt to rider upon fare deduction.
