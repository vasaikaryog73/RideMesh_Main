# Thymeleaf UI (Port 8085)

## Overview
The **Thymeleaf UI** provides the web interface for riders and drivers. It connects to the backend through the central **Spring Cloud API Gateway** (`http://localhost:8089`) using a stateless JWT stored in secure browser cookies.

## Features
- **Branding**: RideMesh logo & modern responsive layout with light/dark glassmorphic themes.
- **Rider Portal**: Request rides with full address autocompletion & Leaflet maps, view real-time matching status, wallet top-up, trip history with search & double-click inline copyable addresses.
- **Driver Terminal**: Toggle duty availability (`Available` / `Unavailable`), view incoming ride requests, accept bookings, verify rider OTP, end rides, and view completed rides with search & double-click address copy.
- **Incident Reporting**: Modal reporting for riders with compulsory evidence media and automated confirmation email.
- **Feedback & Bug Reporting**: Profile modal for Feedback, Suggestions, Complaints, and Bug Reports with dynamic validation and Cloudinary media uploads.
- **Admin Consoles**:
  - **Driver Applications**: Review & approve/reject applicants with email notifications.
  - **Driver Cancellation Requests**: Review deactivation requests with 6-digit OTP verification.
  - **Report Applications**: Moderate driver incident reports, dispatch notices, or execute suspensions.
  - **Improvement Console**: Filter and search feedback/suggestions/complaints/bugs, with interactive email reply capabilities.
- **Authentication**: Login, signup, driver onboarding, and role-based redirection.
