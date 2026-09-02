# Thymeleaf UI (Port 8085)

## Overview
The **Thymeleaf UI** provides the web interface for riders and drivers. It connects to the backend through the central **Spring Cloud API Gateway** (`http://localhost:8089`) using a stateless JWT stored in secure browser cookies.

## Features
- **Branding**: RideMesh logo & modern responsive layout.
- **Rider Portal**: Request rides, view real-time matching status, wallet top-up, trip history.
- **Driver Terminal**: Toggle availability, view available ride requests, accept bookings, verify rider OTP, end rides.
- **Authentication**: Login, signup, driver onboarding, and role-based redirection.
