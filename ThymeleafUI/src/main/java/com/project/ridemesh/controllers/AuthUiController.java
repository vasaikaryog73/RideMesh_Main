package com.project.ridemesh.controllers;

import com.project.ridemesh.client.GatewayClient;
import com.project.ridemesh.dto.LoginRequestDto;
import com.project.ridemesh.dto.LoginResponseDto;
import com.project.ridemesh.dto.SignupDto;
import com.project.ridemesh.dto.UserDto;
import com.project.ridemesh.dto.WalletDto;
import com.project.ridemesh.dto.PageResponse;
import com.project.ridemesh.dto.RideDto;
import com.project.ridemesh.dto.RideRequestDto;
import com.project.ridemesh.dto.PointDto;
import com.project.ridemesh.entities.enums.PaymentMethod;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import com.project.ridemesh.dto.DriverDto;
import com.project.ridemesh.client.GoogleAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class AuthUiController {

    private static final Logger log = LoggerFactory.getLogger(AuthUiController.class);

    private final GatewayClient gatewayClient;
    private final com.project.ridemesh.client.CloudinaryService cloudinaryService;
    private final GoogleAuthService googleAuthService;

    public AuthUiController(GatewayClient gatewayClient, 
                            com.project.ridemesh.client.CloudinaryService cloudinaryService,
                            GoogleAuthService googleAuthService) {
        this.gatewayClient = gatewayClient;
        this.cloudinaryService = cloudinaryService;
        this.googleAuthService = googleAuthService;
    }

    @GetMapping("/")
    public String indexPage(@CookieValue(value = "jwt_token", required = false) String token, Model model) {
        if (token != null && !token.isEmpty()) {
            try {
                UserDto profile = gatewayClient.getProfile(token).block();
                if (profile != null) {
                    model.addAttribute("currentUser", profile);
                }
            } catch (Exception e) {
                // Token invalid/expired, treat as guest
            }
        }
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboardRedirect(@CookieValue(value = "jwt_token", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        try {
            Map<String, Object> payload = parseJwtPayload(token);
            String roleStr = (String) payload.get("role");
            if (roleStr != null && roleStr.contains("ADMIN")) {
                return "redirect:/admin/dashboard";
            }

            UserDto profile = gatewayClient.getProfile(token).block();
            if (profile != null) {
                if (Boolean.FALSE.equals(profile.getEmailVerified())) {
                    return "redirect:/verify-email-notice";
                }
                if (profile.getRoles() != null) {
                    if (profile.getRoles().contains("RIDER")) {
                        return "redirect:/rider/dashboard";
                    } else if (profile.getRoles().contains("DRIVER")) {
                        return "redirect:/driver/dashboard";
                    }
                }
            }
        } catch (Exception e) {
            return "redirect:/logout";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String errorParam,
                            @CookieValue(value = "jwt_token", required = false) String token,
                            Model model) {
        if (token != null && !token.isEmpty()) {
            return "redirect:/dashboard";
        }
        if (errorParam != null && !errorParam.isEmpty()) {
            model.addAttribute("error", errorParam);
        }
        model.addAttribute("loginRequest", new LoginRequestDto());
        return "auth/login";
    }

    @PostMapping("/login")
    public String processLogin(@ModelAttribute LoginRequestDto loginRequest, HttpServletResponse response, Model model) {
        try {
            LoginResponseDto loginResponse = gatewayClient.login(loginRequest).block();
            if (loginResponse != null && loginResponse.getAccessToken() != null) {
                Cookie cookie = new Cookie("jwt_token", loginResponse.getAccessToken());
                cookie.setHttpOnly(true);
                cookie.setPath("/");
                cookie.setMaxAge(60 * 60 * 24);
                response.addCookie(cookie);
                return "redirect:/dashboard";
            }
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            String body = e.getResponseBodyAsString();
            if (body != null && body.contains("Different authentication method in use for this email")) {
                model.addAttribute("error", "Different authentication method in use for this email");
            } else {
                model.addAttribute("error", "Invalid email or password. Please try again.");
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("Different authentication method in use for this email")) {
                model.addAttribute("error", "Different authentication method in use for this email");
            } else {
                model.addAttribute("error", "Invalid email or password. Please try again.");
            }
        }
        model.addAttribute("loginRequest", loginRequest);
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signupPage(Model model) {
        model.addAttribute("signupRequest", new SignupDto());
        return "auth/signup";
    }

    @GetMapping("/auth/check-username")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkUsernameAvailability(@RequestParam("username") String username) {
        Boolean exists = gatewayClient.checkUsernameExists(username).block();
        boolean taken = Boolean.TRUE.equals(exists);
        return ResponseEntity.ok(Map.of("exists", taken, "available", !taken));
    }

    @PostMapping("/signup")
    public String processSignup(@ModelAttribute SignupDto signupRequest, 
                                @RequestParam(value = "pfpFile", required = false) MultipartFile pfpFile,
                                @RequestParam String confirmPassword, 
                                Model model) {
        if (!signupRequest.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("signupRequest", signupRequest);
            return "auth/signup";
        }

        String username = signupRequest.getUsername();
        if (username == null || !username.matches("^[a-zA-Z]{5}#[0-9]{5}$")) {
            model.addAttribute("error", "Username must consist of 5 letters followed by # and 5 numbers (e.g. abcde#12345).");
            model.addAttribute("signupRequest", signupRequest);
            return "auth/signup";
        }

        if (signupRequest.getPhoneNumber() == null || signupRequest.getPhoneNumber().trim().isEmpty() || signupRequest.getPhoneNumber().replaceAll("[^0-9]", "").length() < 10) {
            model.addAttribute("error", "Please provide a valid 10-digit mobile number.");
            model.addAttribute("signupRequest", signupRequest);
            return "auth/signup";
        }

        Boolean exists = gatewayClient.checkUsernameExists(username).block();
        if (Boolean.TRUE.equals(exists)) {
            model.addAttribute("error", "Username is already taken. Please choose another.");
            model.addAttribute("signupRequest", signupRequest);
            return "auth/signup";
        }

        try {
            if (pfpFile != null && !pfpFile.isEmpty()) {
                String uploadedUrl = cloudinaryService.uploadImage(pfpFile);
                if (uploadedUrl != null) {
                    signupRequest.setProfilePictureUrl(uploadedUrl);
                } else {
                    signupRequest.setProfilePictureUrl("https://res.cloudinary.com/domckasfk/image/upload/v1774786542/mediscan/reports/ygnjdwtvacvw6rrzmumc.jpg");
                }
            } else {
                signupRequest.setProfilePictureUrl("https://res.cloudinary.com/domckasfk/image/upload/v1774786542/mediscan/reports/ygnjdwtvacvw6rrzmumc.jpg");
            }

            gatewayClient.signup(signupRequest).block();
            return "redirect:/login?signupSuccess=true";
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            String body = e.getResponseBodyAsString();
            if (body != null && body.contains("Different authentication method in use for this email")) {
                model.addAttribute("error", "Different authentication method in use for this email");
            } else {
                model.addAttribute("error", "Registration failed: Email or username may already be in use.");
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("Different authentication method in use for this email")) {
                model.addAttribute("error", "Different authentication method in use for this email");
            } else {
                model.addAttribute("error", "Registration failed: " + (e.getMessage() != null ? e.getMessage() : "Email or username may already be in use."));
            }
        }
        model.addAttribute("signupRequest", signupRequest);
        return "auth/signup";
    }

    @GetMapping("/auth/google")
    public String initiateGoogleAuth(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String redirectUri = scheme + "://" + serverName + (serverPort == 80 || serverPort == 443 ? "" : ":" + serverPort) + "/login/oauth2/code/google";
        return "redirect:" + googleAuthService.buildAuthorizationUrl(redirectUri);
    }

    @GetMapping({"/login/oauth2/code/google", "/auth/google/callback"})
    public String handleGoogleCallback(@RequestParam(value = "code", required = false) String code,
                                       @RequestParam(value = "error", required = false) String error,
                                       HttpServletRequest request,
                                       HttpServletResponse response,
                                       Model model) {
        if (error != null || code == null || code.trim().isEmpty()) {
            return "redirect:/login?googleError=cancelled";
        }
        try {
            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            String path = request.getRequestURI();
            String redirectUri = scheme + "://" + serverName + (serverPort == 80 || serverPort == 443 ? "" : ":" + serverPort) + path;

            com.project.ridemesh.dto.GoogleAuthDto userInfo = googleAuthService.fetchGoogleUserInfo(code, redirectUri);
            if (userInfo == null || userInfo.getEmail() == null) {
                return "redirect:/login?googleError=failed";
            }

            // Upload Google PFP to Cloudinary
            if (userInfo.getProfilePictureUrl() != null && !userInfo.getProfilePictureUrl().trim().isEmpty()) {
                String cloudinaryUrl = cloudinaryService.uploadFromUrl(userInfo.getProfilePictureUrl());
                if (cloudinaryUrl != null) {
                    userInfo.setProfilePictureUrl(cloudinaryUrl);
                } else {
                    userInfo.setProfilePictureUrl("https://res.cloudinary.com/domckasfk/image/upload/v1774786542/mediscan/reports/ygnjdwtvacvw6rrzmumc.jpg");
                }
            } else {
                userInfo.setProfilePictureUrl("https://res.cloudinary.com/domckasfk/image/upload/v1774786542/mediscan/reports/ygnjdwtvacvw6rrzmumc.jpg");
            }

            LoginResponseDto loginResponse = gatewayClient.googleAuth(userInfo).block();
            if (loginResponse != null && loginResponse.getAccessToken() != null) {
                Cookie cookie = new Cookie("jwt_token", loginResponse.getAccessToken());
                cookie.setHttpOnly(true);
                cookie.setPath("/");
                cookie.setMaxAge(60 * 60 * 24);
                response.addCookie(cookie);
                return "redirect:/dashboard";
            }
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.error("Google OAuth API Error: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            String body = e.getResponseBodyAsString();
            if (body != null && body.contains("Different authentication method in use for this email")) {
                return "redirect:/login?error=Different+authentication+method+in+use+for+this+email";
            }
            return "redirect:/login?googleError=server_error";
        } catch (Exception e) {
            log.error("Google OAuth callback exception: ", e);
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("Different authentication method in use for this email")) {
                return "redirect:/login?error=Different+authentication+method+in+use+for+this+email";
            }
            return "redirect:/login?googleError=server_error";
        }
        return "redirect:/login?googleError=failed";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam("name") String name,
                                @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
                                @RequestParam(value = "pfpFile", required = false) MultipartFile pfpFile,
                                @CookieValue(value = "jwt_token", required = false) String token,
                                RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        try {
            String pfpUrl = null;
            if (pfpFile != null && !pfpFile.isEmpty()) {
                pfpUrl = cloudinaryService.uploadImage(pfpFile);
            }
            gatewayClient.updateProfile(token, name, pfpUrl, phoneNumber).block();
            redirectAttributes.addFlashAttribute("profileSuccess", "Profile updated successfully!");
        } catch (Exception e) {
            log.error("Error updating profile: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("profileError", "Failed to update profile: " + e.getMessage());
        }
        return "redirect:/profile";
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt_token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/login?logout=true";
    }

    @GetMapping("/profile")
    public String profilePage(@CookieValue(value = "jwt_token", required = false) String token, Model model) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        try {
            Map<String, Object> payload = parseJwtPayload(token);
            String roleStr = (String) payload.get("role");
            if (roleStr != null && roleStr.contains("ADMIN")) {
                UserDto dummyUser = new UserDto();
                dummyUser.setId(Long.valueOf(payload.get("sub").toString()));
                dummyUser.setEmail((String) payload.get("email"));
                dummyUser.setName("System Admin");
                dummyUser.setRoles(java.util.Set.of("ADMIN"));
                dummyUser.setEmailVerified(true);
                model.addAttribute("currentUser", dummyUser);
            } else {
                UserDto profile = gatewayClient.getProfile(token).block();
                if (profile == null) return "redirect:/logout";
                model.addAttribute("currentUser", profile);
                if (profile.getRoles() != null && profile.getRoles().contains("DRIVER")) {
                    try {
                        DriverDto driverProfile = gatewayClient.getDriverProfile(token).block();
                        model.addAttribute("driverProfile", driverProfile);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            return "redirect:/logout";
        }
        return "auth/profile";
    }

    // Unverified notice page mapping
    @GetMapping("/verify-email-notice")
    public String verifyEmailNotice(@CookieValue(value = "jwt_token", required = false) String token, Model model) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        try {
            UserDto profile = gatewayClient.getProfile(token).block();
            if (profile == null) {
                return "redirect:/logout";
            }
            if (Boolean.TRUE.equals(profile.getEmailVerified())) {
                return "redirect:/dashboard";
            }
            model.addAttribute("currentUser", profile);
        } catch (Exception e) {
            return "redirect:/logout";
        }
        return "auth/verify-email-notice";
    }

    // Ajax trigger from notice page to gateway send-verification
    @PostMapping("/verify-email-notice/send")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> triggerVerificationEmail(@CookieValue(value = "jwt_token", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("success", false));
        }
        try {
            gatewayClient.sendVerification(token).block();
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false));
        }
    }

    // Verification link handler (signs out and redirects back to login)
    @GetMapping("/verify")
    public String executeVerification(@RequestParam String token, HttpServletResponse response) {
        try {
            gatewayClient.verifyEmail(token).block();

            // Success: clear cookie
            Cookie cookie = new Cookie("jwt_token", "");
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);

            return "redirect:/login?verificationSuccess=true";
        } catch (Exception e) {
            return "redirect:/login?verificationError=true";
        }
    }

    // Dashboard views secured with email-verification redirects
    @GetMapping("/rider/dashboard")
    public String riderDashboard(@CookieValue(value = "jwt_token", required = false) String token, 
                                 jakarta.servlet.http.HttpSession session, 
                                 Model model) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        try {
            UserDto profile = gatewayClient.getProfile(token).block();
            if (profile == null) return "redirect:/logout";
            if (Boolean.FALSE.equals(profile.getEmailVerified())) {
                return "redirect:/verify-email-notice";
            }
            if (profile.getPhoneNumber() == null || profile.getPhoneNumber().trim().isEmpty()) {
                model.addAttribute("missingPhone", true);
            }
            model.addAttribute("currentUser", profile);

            try {
                WalletDto wallet = gatewayClient.getWalletBalance(token).block();
                model.addAttribute("wallet", wallet);
            } catch (Exception we) {
                WalletDto emptyWallet = new WalletDto();
                emptyWallet.setBalance(0.0);
                model.addAttribute("wallet", emptyWallet);
            }

            RideDto activeRide = null;
            try {
                activeRide = gatewayClient.getActiveRideRider(token).block();
            } catch (Exception re) {
                // Ignore
            }

            if (activeRide != null) {
                model.addAttribute("activeRide", activeRide);
                session.removeAttribute("activeRequest");
            } else {
                RideRequestDto activeRequest = (RideRequestDto) session.getAttribute("activeRequest");
                if (activeRequest != null && activeRequest.getRequestTime() != null) {
                    long elapsedSeconds = java.time.Duration.between(activeRequest.getRequestTime(), java.time.LocalDateTime.now()).getSeconds();
                    long remainingSeconds = 300 - elapsedSeconds;
                    if (remainingSeconds <= 0) {
                        // 5 minutes elapsed: auto-cancel request!
                        session.removeAttribute("activeRequest");
                        session.removeAttribute("bookingError");
                        model.addAttribute("activeRequest", null);
                    } else {
                        model.addAttribute("activeRequest", activeRequest);
                        model.addAttribute("remainingSeconds", remainingSeconds);
                    }
                } else {
                    model.addAttribute("activeRequest", activeRequest);
                }
            }

            model.addAttribute("bookingError", session.getAttribute("bookingError"));
            session.removeAttribute("bookingError");
        } catch (Exception e) {
            return "redirect:/logout";
        }
        return "rider/dashboard";
    }

    @PostMapping("/rider/requestRide")
    public String requestRide(@CookieValue(value = "jwt_token", required = false) String token,
                              @RequestParam double pickupLat,
                              @RequestParam double pickupLng,
                              @RequestParam double dropoffLat,
                              @RequestParam double dropoffLng,
                              @RequestParam(required = false) String pickupAddress,
                              @RequestParam(required = false) String dropoffAddress,
                              @RequestParam String vehicleType,
                              @RequestParam PaymentMethod paymentMethod,
                              jakarta.servlet.http.HttpSession session) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        try {
            UserDto profile = gatewayClient.getProfile(token).block();
            if (profile == null || profile.getPhoneNumber() == null || profile.getPhoneNumber().trim().isEmpty()) {
                session.setAttribute("bookingError", "Please provide a phone number in your profile before booking a ride.");
                return "redirect:/rider/dashboard";
            }

            RideRequestDto req = new RideRequestDto();
            req.setPickUpLocation(new PointDto(new double[]{pickupLng, pickupLat}));
            req.setDropOffLocation(new PointDto(new double[]{dropoffLng, dropoffLat}));
            req.setPickupAddress(pickupAddress);
            req.setDropoffAddress(dropoffAddress);
            req.setVehicleType(vehicleType);
            req.setPaymentMethod(paymentMethod);

            RideRequestDto response = gatewayClient.requestRide(token, req).block();
            session.setAttribute("activeRequest", response);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            String errorMsg = e.getMessage();
            try {
                com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(e.getResponseBodyAsString());
                if (node.has("apiError") && node.get("apiError").has("message")) {
                    errorMsg = node.get("apiError").get("message").asText();
                } else if (node.has("message")) {
                    errorMsg = node.get("message").asText();
                }
            } catch (Exception ignored) {}
            session.setAttribute("bookingError", errorMsg);
        } catch (Exception e) {
            session.setAttribute("bookingError", "Booking failed: " + e.getMessage());
        }
        return "redirect:/rider/dashboard";
    }

    @PostMapping("/rider/cancelRequest")
    public String cancelActiveRequest(@CookieValue(value = "jwt_token", required = false) String token,
                                      jakarta.servlet.http.HttpSession session) {
        RideRequestDto activeRequest = (RideRequestDto) session.getAttribute("activeRequest");
        if (activeRequest != null && activeRequest.getId() != null && token != null && !token.isEmpty()) {
            try {
                gatewayClient.cancelRideRequest(token, activeRequest.getId()).block();
                log.info("Successfully cancelled active ride request ID {} in backend", activeRequest.getId());
            } catch (Exception e) {
                log.error("Failed to cancel active ride request in backend: {}", e.getMessage());
            }
        }
        session.removeAttribute("activeRequest");
        session.removeAttribute("bookingError");
        return "redirect:/rider/dashboard";
    }

    @GetMapping("/driver/dashboard")
    public String driverDashboard(@CookieValue(value = "jwt_token", required = false) String token, 
                                  jakarta.servlet.http.HttpSession session, 
                                  HttpServletResponse httpServletResponse,
                                  Model model) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        try {
            UserDto profile = gatewayClient.getProfile(token).block();
            if (profile == null) return "redirect:/logout";
            if (Boolean.FALSE.equals(profile.getEmailVerified())) {
                return "redirect:/verify-email-notice";
            }
            if (profile.getRoles() == null || !profile.getRoles().contains("DRIVER")) {
                session.setAttribute("riderError", "You are not an approved driver yet. Please check your application status or apply to become a driver.");
                return "redirect:/become-a-driver";
            }

            // Auto-refresh JWT if token was issued before driver approval (lacks DRIVER role)
            boolean needsTokenRefresh = false;
            try {
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
                    if (!payload.contains("DRIVER")) {
                        needsTokenRefresh = true;
                    }
                }
            } catch (Exception ignored) {}

            if (needsTokenRefresh) {
                try {
                    LoginResponseDto refreshed = gatewayClient.refreshTokenForUser(profile.getId()).block();
                    if (refreshed != null && refreshed.getAccessToken() != null) {
                        token = refreshed.getAccessToken();
                        Cookie refreshedCookie = new Cookie("jwt_token", token);
                        refreshedCookie.setHttpOnly(true);
                        refreshedCookie.setPath("/");
                        refreshedCookie.setMaxAge(60 * 60 * 24);
                        httpServletResponse.addCookie(refreshedCookie);
                        log.info("Successfully refreshed JWT token for driver user ID {}", profile.getId());
                    }
                } catch (Exception e) {
                    log.warn("Failed to auto-refresh token for driver user ID {}: {}", profile.getId(), e.getMessage());
                }
            }

            model.addAttribute("currentUser", profile);
            DriverDto driverProfile = null;
            try {
                driverProfile = gatewayClient.getDriverProfile(token).block();
            } catch (Exception ignored) {}
            model.addAttribute("driverProfile", driverProfile);

            RideDto activeRide = null;
            try {
                activeRide = gatewayClient.getActiveRideDriver(token).block();
            } catch (Exception re) {
                // Ignore
            }

            if (activeRide != null) {
                model.addAttribute("activeRide", activeRide);
                session.setAttribute("activeDriverRide", activeRide);
            } else {
                session.removeAttribute("activeDriverRide");
                model.addAttribute("activeRide", null);

                try {
                    List<RideRequestDto> pendingRides = java.util.Collections.emptyList();
                    // Only query matchmaking pending rides if the driver is marked Available
                    if (driverProfile != null && Boolean.TRUE.equals(driverProfile.getAvailable())) {
                        pendingRides = gatewayClient.getPendingRidesNearDriver(token).block();
                        if (pendingRides != null) {
                            @SuppressWarnings("unchecked")
                            java.util.Set<Long> declinedRideIds = (java.util.Set<Long>) session.getAttribute("declinedRideIds");
                            if (declinedRideIds != null && !declinedRideIds.isEmpty()) {
                                pendingRides = pendingRides.stream()
                                        .filter(r -> r.getId() == null || !declinedRideIds.contains(r.getId()))
                                        .collect(java.util.stream.Collectors.toList());
                            }
                        }
                    }
                    log.info("Fetched {} pending rides for driver", pendingRides != null ? pendingRides.size() : 0);
                    model.addAttribute("pendingRides", pendingRides != null ? pendingRides : java.util.Collections.emptyList());
                } catch (Exception pre) {
                    log.error("Failed to fetch pending rides near driver: ", pre);
                    model.addAttribute("pendingRides", java.util.Collections.emptyList());
                }
            }

            model.addAttribute("driverError", session.getAttribute("driverError"));
            model.addAttribute("driverSuccess", session.getAttribute("driverSuccess"));
            session.removeAttribute("driverError");
            session.removeAttribute("driverSuccess");
        } catch (Exception e) {
            return "redirect:/logout";
        }
        return "driver/dashboard";
    }

    @PostMapping("/driver/acceptRide")
    public String acceptRide(@CookieValue(value = "jwt_token", required = false) String token,
                             @RequestParam Long rideRequestId,
                             jakarta.servlet.http.HttpSession session) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        try {
            RideDto ride = gatewayClient.acceptRide(token, rideRequestId).block();
            session.setAttribute("activeDriverRide", ride);
            session.setAttribute("driverSuccess", "Successfully accepted Ride Request #" + rideRequestId + "! Please meet the rider.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            String errorMsg = e.getResponseBodyAsString();
            session.setAttribute("driverError", "Failed to accept ride: " + (errorMsg.isEmpty() ? e.getStatusText() : errorMsg));
        } catch (Exception e) {
            session.setAttribute("driverError", "Failed to accept ride request: " + e.getMessage());
        }
        return "redirect:/driver/dashboard";
    }

    @PostMapping("/driver/declineRide")
    public String declineRide(@CookieValue(value = "jwt_token", required = false) String token,
                              @RequestParam Long rideRequestId,
                              jakarta.servlet.http.HttpSession session) {
        if (rideRequestId != null) {
            @SuppressWarnings("unchecked")
            java.util.Set<Long> declinedRideIds = (java.util.Set<Long>) session.getAttribute("declinedRideIds");
            if (declinedRideIds == null) {
                declinedRideIds = new java.util.HashSet<>();
            }
            declinedRideIds.add(rideRequestId);
            session.setAttribute("declinedRideIds", declinedRideIds);
        }
        if (token != null && !token.isEmpty() && rideRequestId != null) {
            try {
                gatewayClient.declineRide(token, rideRequestId).block();
                session.setAttribute("driverSuccess", "Declined Ride Request #" + rideRequestId + ".");
            } catch (Exception e) {
                log.warn("Failed to decline ride request {}: {}", rideRequestId, e.getMessage());
            }
        }
        return "redirect:/driver/dashboard";
    }

    @PostMapping("/driver/startRide")
    public String startRide(@CookieValue(value = "jwt_token", required = false) String token,
                            @RequestParam Long rideId,
                            @RequestParam String otp,
                            jakarta.servlet.http.HttpSession session) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        try {
            RideDto ride = gatewayClient.startRide(token, rideId, otp).block();
            session.setAttribute("activeDriverRide", ride);
            session.setAttribute("driverSuccess", "Ride started successfully! You are now en route to the destination.");
        } catch (Exception e) {
            session.setAttribute("driverError", "Failed to start ride. Please verify that the OTP verification code is correct.");
        }
        return "redirect:/driver/dashboard";
    }

    @PostMapping("/driver/endRide")
    public String endRide(@CookieValue(value = "jwt_token", required = false) String token,
                          @RequestParam Long rideId,
                          jakarta.servlet.http.HttpSession session,
                          org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        try {
            gatewayClient.endRide(token, rideId).block();
            session.removeAttribute("activeDriverRide");
            redirectAttributes.addFlashAttribute("successMessage", "Ride completed successfully! Earnings have been credited to your wallet.");
            return "redirect:/driver/history";
        } catch (Exception e) {
            session.setAttribute("driverError", "Failed to end the ride: " + e.getMessage());
            return "redirect:/driver/dashboard";
        }
    }

    @PostMapping("/driver/cancelRide")
    public String cancelDriverRide(@CookieValue(value = "jwt_token", required = false) String token,
                                   @RequestParam Long rideId,
                                   jakarta.servlet.http.HttpSession session) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        try {
            gatewayClient.cancelDriverRide(token, rideId).block();
            session.removeAttribute("activeDriverRide");
            session.setAttribute("driverSuccess", "Ride was cancelled successfully.");
        } catch (Exception e) {
            session.setAttribute("driverError", "Failed to cancel the ride: " + e.getMessage());
        }
        return "redirect:/driver/dashboard";
    }

    @PostMapping("/driver/rateRider/{rideId}")
    public String rateRider(@CookieValue(value = "jwt_token", required = false) String token,
                            @PathVariable Long rideId,
                            @RequestParam Double rating,
                            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        try {
            gatewayClient.rateRider(token, rideId, rating).block();
            redirectAttributes.addFlashAttribute("successMessage", "Rider rated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to submit rating: " + e.getMessage());
        }
        return "redirect:/driver/history";
    }

    @GetMapping("/rider/wallet")
    public String riderWallet(@CookieValue(value = "jwt_token", required = false) String token, Model model) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        UserDto profile;
        try {
            profile = gatewayClient.getProfile(token).block();
            if (profile == null) return "redirect:/logout";
            if (Boolean.FALSE.equals(profile.getEmailVerified())) {
                return "redirect:/verify-email-notice";
            }
            model.addAttribute("currentUser", profile);
        } catch (Exception e) {
            return "redirect:/logout";
        }

        try {
            WalletDto wallet = gatewayClient.getWalletBalance(token).block();
            model.addAttribute("wallet", wallet);
        } catch (Exception e) {
            WalletDto emptyWallet = new WalletDto();
            emptyWallet.setBalance(0.0);
            emptyWallet.setTransactions(java.util.Collections.emptyList());
            model.addAttribute("wallet", emptyWallet);
            model.addAttribute("error", "Wallet service is currently unavailable.");
        }
        return "rider/wallet";
    }

    @PostMapping("/rider/wallet/create-order")
    @ResponseBody
    public ResponseEntity<?> createOrder(@CookieValue(value = "jwt_token", required = false) String token, 
                                         @RequestBody Map<String, Double> request) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        Double amount = request.get("amount");
        try {
            Map<String, Object> order = gatewayClient.createRazorpayOrder(token, amount).block();
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/rider/wallet/verify-payment")
    @ResponseBody
    public ResponseEntity<?> verifyPayment(@CookieValue(value = "jwt_token", required = false) String token, 
                                           @RequestBody Map<String, String> request) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        try {
            WalletDto wallet = gatewayClient.verifyRazorpayPayment(token, request).block();
            return ResponseEntity.ok(wallet);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/rider/wallet/create-withdraw-order")
    @ResponseBody
    public ResponseEntity<?> createWithdrawOrder(@CookieValue(value = "jwt_token", required = false) String token, 
                                                 @RequestBody Map<String, Double> request) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        Double amount = request.get("amount");
        try {
            Map<String, Object> order = gatewayClient.createRazorpayWithdrawOrder(token, amount).block();
            return ResponseEntity.ok(order);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            String body = e.getResponseBodyAsString();
            return ResponseEntity.status(e.getStatusCode()).body(body.isEmpty() ? e.getMessage() : body);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/rider/wallet/verify-withdraw")
    @ResponseBody
    public ResponseEntity<?> verifyWithdraw(@CookieValue(value = "jwt_token", required = false) String token, 
                                            @RequestBody Map<String, String> request) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        try {
            WalletDto wallet = gatewayClient.verifyRazorpayWithdraw(token, request).block();
            return ResponseEntity.ok(wallet);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            String body = e.getResponseBodyAsString();
            return ResponseEntity.status(e.getStatusCode()).body(body.isEmpty() ? e.getMessage() : body);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/rider/confirmCashPaid/{rideId}")
    public String confirmCashPaid(@CookieValue(value = "jwt_token", required = false) String token,
                                  @PathVariable Long rideId,
                                  jakarta.servlet.http.HttpSession session) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        try {
            gatewayClient.confirmCashPaid(token, rideId).block();
            session.setAttribute("bookingSuccess", "You marked cash as paid to the driver.");
        } catch (Exception e) {
            session.setAttribute("bookingError", "Failed to confirm cash paid: " + e.getMessage());
        }
        return "redirect:/rider/dashboard";
    }

    @PostMapping("/driver/confirmCashReceived/{rideId}")
    public String confirmCashReceived(@CookieValue(value = "jwt_token", required = false) String token,
                                      @PathVariable Long rideId,
                                      jakarta.servlet.http.HttpSession session) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        try {
            gatewayClient.confirmCashReceived(token, rideId).block();
            session.setAttribute("driverSuccess", "Payment received! Cash trip successfully finalized.");
        } catch (Exception e) {
            session.setAttribute("driverError", "Failed to confirm cash received: " + e.getMessage());
        }
        return "redirect:/driver/dashboard";
    }

    @PostMapping("/rider/ride/create-upi-order")
    @ResponseBody
    public ResponseEntity<?> createRideUpiOrder(@CookieValue(value = "jwt_token", required = false) String token,
                                               @RequestBody Map<String, Double> request) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        Double amount = request.get("amount");
        try {
            Map<String, Object> order = gatewayClient.createRazorpayOrder(token, amount).block();
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/rider/ride/verify-upi-payment")
    @ResponseBody
    public ResponseEntity<?> verifyRideUpiPayment(@CookieValue(value = "jwt_token", required = false) String token,
                                                  @RequestBody Map<String, Object> request) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        try {
            Map<String, Object> upiResult = gatewayClient.payRideUpi(token, request).block();
            Long rideId = request.get("rideId") != null ? Long.parseLong(request.get("rideId").toString()) : null;
            if (rideId != null) {
                try {
                    gatewayClient.finalizeUpiPayment(token, rideId).block();
                } catch (Exception fe) {
                    log.warn("Failed to finalize ride {} status: {}", rideId, fe.getMessage());
                }
            }
            return ResponseEntity.ok(upiResult);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/rider/history")
    public String riderHistory(@CookieValue(value = "jwt_token", required = false) String token, 
                               @RequestParam(defaultValue = "0") int page, 
                               Model model) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        UserDto profile;
        try {
            profile = gatewayClient.getProfile(token).block();
            if (profile == null) return "redirect:/logout";
            if (Boolean.FALSE.equals(profile.getEmailVerified())) {
                return "redirect:/verify-email-notice";
            }
            model.addAttribute("currentUser", profile);
        } catch (Exception e) {
            return "redirect:/logout";
        }

        try {
            PageResponse<RideDto> ridesPage = gatewayClient.getRiderRides(token, page, 10).block();
            model.addAttribute("ridesPage", ridesPage);
        } catch (Exception e) {
            PageResponse<RideDto> emptyPage = new PageResponse<>();
            emptyPage.setContent(java.util.Collections.emptyList());
            emptyPage.setTotalPages(0);
            emptyPage.setTotalElements(0);
            model.addAttribute("ridesPage", emptyPage);
            model.addAttribute("error", "Ride history service is currently unavailable.");
        }
        model.addAttribute("currentPage", page);
        return "rider/history";
    }

    @GetMapping("/driver/history")
    public String driverHistory(@CookieValue(value = "jwt_token", required = false) String token, 
                                @RequestParam(defaultValue = "0") int page, 
                                Model model) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        UserDto profile;
        try {
            profile = gatewayClient.getProfile(token).block();
            if (profile == null) return "redirect:/logout";
            if (Boolean.FALSE.equals(profile.getEmailVerified())) {
                return "redirect:/verify-email-notice";
            }
            model.addAttribute("currentUser", profile);
        } catch (Exception e) {
            return "redirect:/logout";
        }

        try {
            PageResponse<RideDto> ridesPage = gatewayClient.getDriverRides(token, page, 10).block();
            model.addAttribute("ridesPage", ridesPage);
        } catch (Exception e) {
            PageResponse<RideDto> emptyPage = new PageResponse<>();
            emptyPage.setContent(java.util.Collections.emptyList());
            emptyPage.setTotalPages(0);
            emptyPage.setTotalElements(0);
            model.addAttribute("ridesPage", emptyPage);
            model.addAttribute("error", "Ride history service is currently unavailable.");
        }
        model.addAttribute("currentPage", page);
        return "driver/history";
    }

    @GetMapping("/admin-login")
    public String adminLoginPage() {
        return "auth/admin-login";
    }

    @PostMapping("/admin-login")
    public String processAdminLogin(@RequestParam String email, 
                                    @RequestParam String password, 
                                    HttpServletResponse response, 
                                    Model model) {
        try {
            LoginRequestDto loginRequest = new LoginRequestDto(email, password);
            LoginResponseDto loginResponse = gatewayClient.login(loginRequest).block();
            if (loginResponse != null && loginResponse.getAccessToken() != null) {
                String token = loginResponse.getAccessToken();
                Map<String, Object> payload = parseJwtPayload(token);
                String roleStr = (String) payload.get("role");
                if (roleStr != null && roleStr.contains("ADMIN")) {
                    Cookie cookie = new Cookie("jwt_token", token);
                    cookie.setHttpOnly(true);
                    cookie.setPath("/");
                    cookie.setMaxAge(60 * 60 * 24);
                    response.addCookie(cookie);
                    return "redirect:/admin/dashboard";
                } else {
                    model.addAttribute("error", "Access Denied: Not an administrator.");
                }
            } else {
                model.addAttribute("error", "Invalid email or password.");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Invalid credentials or server connection error.");
        }
        return "auth/admin-login";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(@CookieValue(value = "jwt_token", required = false) String token, Model model) {
        if (token == null || token.isEmpty()) {
            return "redirect:/admin-login";
        }
        try {
            Map<String, Object> payload = parseJwtPayload(token);
            String roleStr = (String) payload.get("role");
            if (roleStr == null || !roleStr.contains("ADMIN")) {
                return "redirect:/logout";
            }
            
            // Build a dummy UserDto containing the email and name so the UI page loads correctly!
            UserDto dummyUser = new UserDto();
            dummyUser.setId(Long.valueOf(payload.get("sub").toString()));
            dummyUser.setEmail((String) payload.get("email"));
            dummyUser.setName("System Admin");
            dummyUser.setRoles(java.util.Set.of("ADMIN"));
            dummyUser.setEmailVerified(true);
            
            model.addAttribute("currentUser", dummyUser);
        } catch (Exception e) {
            return "redirect:/logout";
        }
        return "admin/dashboard";
    }

    @PostMapping("/admin/onboardDriver")
    public String onboardDriver(@CookieValue(value = "jwt_token", required = false) String token,
                                @RequestParam Long userId,
                                @RequestParam Long vehicleId,
                                org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/admin-login";
        }
        try {
            gatewayClient.onboardDriver(token, userId, vehicleId).block();
            redirectAttributes.addFlashAttribute("successMessage", "Driver with User ID: " + userId + " onboarded successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to onboard driver. Make sure User ID exists, is verified, and the vehicle ID is a valid number.");
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/rider/cancelRide/{rideId}")
    public String cancelConfirmedRide(@CookieValue(value = "jwt_token", required = false) String token,
                                      @PathVariable Long rideId,
                                      org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        try {
            gatewayClient.cancelRide(token, rideId).block();
            redirectAttributes.addFlashAttribute("successMessage", "Ride #" + rideId + " cancelled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to cancel ride: " + e.getMessage());
        }
        return "redirect:/rider/history";
    }

    @PostMapping("/rider/rateDriver/{rideId}")
    public String rateDriver(@CookieValue(value = "jwt_token", required = false) String token,
                             @PathVariable Long rideId,
                             @RequestParam Double rating,
                             @RequestParam(required = false) Long driverId,
                             org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        try {
            boolean userRecorded = false;
            try {
                gatewayClient.rateDriver(token, rideId, rating, driverId).block();
                userRecorded = true;
            } catch (Exception e) {
                log.warn("UserService rateDriver exception: {}", e.getMessage());
            }

            boolean rideRecorded = false;
            try {
                gatewayClient.recordRideRating(token, rideId, rating).block();
                rideRecorded = true;
            } catch (Exception rre) {
                log.warn("Failed to record ride rating in RideService: {}", rre.getMessage());
            }

            if (userRecorded || rideRecorded) {
                redirectAttributes.addFlashAttribute("successMessage", "Rating submitted successfully!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Rating has already been recorded for this ride.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to submit rating: " + e.getMessage());
        }
        return "redirect:/rider/history";
    }

    @GetMapping("/rider/become-driver")
    public String becomeDriverPage(@CookieValue(value = "jwt_token", required = false) String token, Model model) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        try {
            UserDto profile = gatewayClient.getProfile(token).block();
            if (profile == null) return "redirect:/logout";
            model.addAttribute("currentUser", profile);

            try {
                com.project.ridemesh.dto.DriverApplicationDto myApp = gatewayClient.getMyDriverApplication(token, profile.getId()).block();
                if (myApp != null && myApp.getId() != null) {
                    model.addAttribute("myApplication", myApp);
                }
            } catch (Exception e) {
                log.error("Error fetching myApplication: ", e);
            }
        } catch (Exception e) {
            return "redirect:/logout";
        }
        return "rider/become-driver";
    }

    @PostMapping("/rider/become-driver")
    public String processBecomeDriver(@CookieValue(value = "jwt_token", required = false) String token,
                                      @RequestParam String vehicleModel,
                                      @RequestParam String vehicleId,
                                      @RequestParam String licenseNumber,
                                      @RequestParam String vehicleType,
                                      @RequestParam String phoneNumber,
                                      @RequestParam String state,
                                      @RequestParam String district,
                                      @RequestParam("licensePhoto") org.springframework.web.multipart.MultipartFile licensePhoto,
                                      @RequestParam("vehiclePhotos") java.util.List<org.springframework.web.multipart.MultipartFile> vehiclePhotos,
                                      @RequestParam("identityPhoto") org.springframework.web.multipart.MultipartFile identityPhoto,
                                      @RequestParam String identityDocType,
                                      org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        try {
            UserDto profile = gatewayClient.getProfile(token).block();
            if (profile != null && profile.getPhoneNumber() != null && !profile.getPhoneNumber().trim().isEmpty()) {
                String registeredDigits = profile.getPhoneNumber().replaceAll("[^0-9]", "");
                String submittedDigits = phoneNumber != null ? phoneNumber.replaceAll("[^0-9]", "") : "";
                boolean matches = false;
                if (registeredDigits.length() >= 10 && submittedDigits.length() >= 10) {
                    matches = registeredDigits.substring(registeredDigits.length() - 10).equals(submittedDigits.substring(submittedDigits.length() - 10));
                } else {
                    matches = registeredDigits.equals(submittedDigits);
                }
                if (!matches) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Mobile number entered (" + phoneNumber + ") must match your registered mobile number (" + profile.getPhoneNumber() + ").");
                    return "redirect:/rider/become-driver";
                }
            }

            String uploadedUrl = cloudinaryService.uploadImage(licensePhoto);
            if (uploadedUrl == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload license document photo. Please select a valid image file.");
                return "redirect:/rider/become-driver";
            }
            String identityUrl = cloudinaryService.uploadImage(identityPhoto);
            if (identityUrl == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload identity proof document photo. Please select a valid image file.");
                return "redirect:/rider/become-driver";
            }
            java.util.List<String> vehicleUrls = new java.util.ArrayList<>();
            if (vehiclePhotos != null) {
                for (org.springframework.web.multipart.MultipartFile photo : vehiclePhotos) {
                    if (!photo.isEmpty()) {
                        String url = cloudinaryService.uploadImage(photo);
                        if (url != null) {
                            vehicleUrls.add(url);
                        }
                    }
                }
            }
            if (vehicleUrls.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please upload at least one vehicle photo.");
                return "redirect:/rider/become-driver";
            }
            String vehiclePhotosJoined = String.join(",", vehicleUrls);

            com.project.ridemesh.dto.DriverApplicationDto dto = new com.project.ridemesh.dto.DriverApplicationDto();
            dto.setVehicleModel(vehicleModel);
            dto.setVehicleId(vehicleId);
            dto.setLicenseNumber(licenseNumber);
            dto.setLicensePhotoUrl(uploadedUrl);
            dto.setVehicleType(vehicleType);
            dto.setPhoneNumber(phoneNumber);
            dto.setState(state);
            dto.setDistrict(district);
            dto.setVehiclePhotos(vehiclePhotosJoined);
            dto.setIdentityPhotoUrl(identityUrl);
            dto.setIdentityDocType(identityDocType);

            gatewayClient.submitDriverApplication(token, dto).block();
            redirectAttributes.addFlashAttribute("successMessage", "We've successfully received your application of becoming a driver. We'll go through and verify your submitted details and documents and will inform you shortly.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to submit driver application: " + e.getMessage());
        }
        return "redirect:/rider/become-driver";
    }

    @GetMapping("/admin/applications")
    public String adminApplicationsPage(@CookieValue(value = "jwt_token", required = false) String token, Model model) {
        if (token == null || token.isEmpty()) {
            return "redirect:/admin-login";
        }
        try {
            Map<String, Object> claims = parseJwtPayload(token);
            String roleStr = (String) claims.get("role");
            if (roleStr == null || !roleStr.contains("ADMIN")) {
                return "redirect:/error/404";
            }
            List<com.project.ridemesh.dto.DriverApplicationDto> apps = gatewayClient.getDriverApplications(token).block();
            model.addAttribute("applications", apps);
            
            UserDto adminUser = new UserDto(999L, "System Admin", "admin@ridemesh.com", java.util.Set.of("ADMIN"), true);
            model.addAttribute("currentUser", adminUser);
        } catch (Exception e) {
            return "redirect:/admin-login";
        }
        return "admin/applications";
    }

    @PostMapping("/admin/applications/{id}/approve")
    public String approveApplication(@CookieValue(value = "jwt_token", required = false) String token,
                                     @PathVariable Long id,
                                     org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/admin-login";
        }
        try {
            gatewayClient.approveDriverApplication(token, id).block();
            redirectAttributes.addFlashAttribute("successMessage", "Application #" + id + " has been approved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to approve application: " + e.getMessage());
        }
        return "redirect:/admin/applications";
    }

    @PostMapping("/admin/applications/{id}/reject")
    public String rejectApplication(@CookieValue(value = "jwt_token", required = false) String token,
                                     @PathVariable Long id,
                                     org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/admin-login";
        }
        try {
            gatewayClient.rejectDriverApplication(token, id).block();
            redirectAttributes.addFlashAttribute("successMessage", "Application #" + id + " has been rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reject application: " + e.getMessage());
        }
        return "redirect:/admin/applications";
    }

    @GetMapping("/admin/drivers")
    public String adminDriversPage(@CookieValue(value = "jwt_token", required = false) String token, Model model) {
        if (token == null || token.isEmpty()) {
            return "redirect:/admin-login";
        }
        try {
            Map<String, Object> claims = parseJwtPayload(token);
            String roleStr = (String) claims.get("role");
            if (roleStr == null || !roleStr.contains("ADMIN")) {
                return "redirect:/error/404";
            }
            List<DriverDto> drivers = gatewayClient.getActiveDrivers(token).block();
            model.addAttribute("drivers", drivers);

            UserDto adminUser = new UserDto(999L, "System Admin", "admin@ridemesh.com", java.util.Set.of("ADMIN"), true);
            model.addAttribute("currentUser", adminUser);
        } catch (Exception e) {
            return "redirect:/admin-login";
        }
        return "admin/drivers";
    }

    @PostMapping("/admin/drivers/{userId}/suspend")
    public String suspendDriver(@CookieValue(value = "jwt_token", required = false) String token,
                                @PathVariable Long userId,
                                org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/admin-login";
        }
        try {
            gatewayClient.suspendDriver(token, userId).block();
            redirectAttributes.addFlashAttribute("successMessage", "Driver profile suspended successfully. User role has been reverted to Rider.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to suspend driver: " + e.getMessage());
        }
        return "redirect:/admin/drivers";
    }

    // --- Driver Availability & Cancellation Endpoints ---

    @PostMapping("/driver/toggle-availability")
    @ResponseBody
    public ResponseEntity<?> toggleDriverAvailability(@CookieValue(value = "jwt_token", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }
        try {
            Map<String, Object> result = gatewayClient.toggleDriverAvailability(token).block();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to toggle driver availability", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/driver/cancellation/send-otp")
    @ResponseBody
    public ResponseEntity<?> sendCancellationOtp(@CookieValue(value = "jwt_token", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }
        try {
            Map<String, Object> result = gatewayClient.sendCancellationOtp(token).block();
            return ResponseEntity.ok(result != null ? result : Map.of("success", true));
        } catch (Exception e) {
            log.error("Failed to send cancellation OTP", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/driver/cancellation/submit")
    @ResponseBody
    public ResponseEntity<?> submitCancellation(
            @CookieValue(value = "jwt_token", required = false) String token,
            @RequestParam("reason") String reason,
            @RequestParam("otp") String otp,
            @RequestParam(value = "media", required = false) MultipartFile media) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }
        try {
            String mediaUrl = null;
            if (media != null && !media.isEmpty()) {
                mediaUrl = cloudinaryService.uploadImage(media);
            }
            com.project.ridemesh.dto.DriverCancellationSubmitDto submitDto =
                    new com.project.ridemesh.dto.DriverCancellationSubmitDto(otp, reason, mediaUrl);
            com.project.ridemesh.dto.DriverCancellationRequestDto result =
                    gatewayClient.submitCancellationRequest(token, submitDto).block();
            return ResponseEntity.ok(Map.of("success", true, "message", "Your driver cancellation request has been submitted to Admin."));
        } catch (Exception e) {
            log.error("Failed to submit cancellation request", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- Admin Cancellation Requests & Reports Endpoints ---

    @GetMapping("/admin/cancellations")
    public String adminCancellationsPage(@CookieValue(value = "jwt_token", required = false) String token, Model model) {
        if (token == null || token.isEmpty()) {
            return "redirect:/admin-login";
        }
        try {
            Map<String, Object> claims = parseJwtPayload(token);
            String roleStr = (String) claims.get("role");
            if (roleStr == null || !roleStr.contains("ADMIN")) {
                return "redirect:/error/404";
            }
            List<com.project.ridemesh.dto.DriverCancellationRequestDto> list =
                    gatewayClient.getCancellationRequests(token).block();
            model.addAttribute("cancellations", list != null ? list : java.util.Collections.emptyList());

            UserDto adminUser = new UserDto(999L, "System Admin", "admin@ridemesh.com", java.util.Set.of("ADMIN"), true);
            model.addAttribute("currentUser", adminUser);
        } catch (Exception e) {
            return "redirect:/admin-login";
        }
        return "admin/cancellations";
    }

    @PostMapping("/admin/cancellations/{id}/approve")
    public String approveCancellation(@CookieValue(value = "jwt_token", required = false) String token,
                                      @PathVariable Long id,
                                      RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/admin-login";
        }
        try {
            gatewayClient.approveCancellationRequest(token, id).block();
            redirectAttributes.addFlashAttribute("successMessage", "Cancellation request #" + id + " has been approved. The driver role has been removed and the user is now a Rider.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to approve cancellation: " + e.getMessage());
        }
        return "redirect:/admin/cancellations";
    }

    @PostMapping("/admin/cancellations/{id}/reject")
    public String rejectCancellation(@CookieValue(value = "jwt_token", required = false) String token,
                                     @PathVariable Long id,
                                     RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/admin-login";
        }
        try {
            gatewayClient.rejectCancellationRequest(token, id).block();
            redirectAttributes.addFlashAttribute("successMessage", "Cancellation request #" + id + " has been rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reject cancellation: " + e.getMessage());
        }
        return "redirect:/admin/cancellations";
    }

    @GetMapping("/admin/reports")
    public String adminReportsPage(@CookieValue(value = "jwt_token", required = false) String token, Model model) {
        if (token == null || token.isEmpty()) {
            return "redirect:/admin-login";
        }
        try {
            Map<String, Object> claims = parseJwtPayload(token);
            String roleStr = (String) claims.get("role");
            if (roleStr == null || !roleStr.contains("ADMIN")) {
                return "redirect:/error/404";
            }
            UserDto adminUser = new UserDto(999L, "System Admin", "admin@ridemesh.com", java.util.Set.of("ADMIN"), true);
            model.addAttribute("currentUser", adminUser);

            List<com.project.ridemesh.dto.DriverReportDto> reports = gatewayClient.getDriverReports(token).block();
            model.addAttribute("reports", reports != null ? reports : java.util.Collections.emptyList());
        } catch (Exception e) {
            log.error("Failed to load admin reports", e);
            model.addAttribute("reports", java.util.Collections.emptyList());
            model.addAttribute("errorMessage", "Failed to load reports: " + e.getMessage());
        }
        return "admin/reports";
    }

    @PostMapping("/rider/report-driver")
    @ResponseBody
    public ResponseEntity<?> submitDriverReport(
            @CookieValue(value = "jwt_token", required = false) String token,
            @RequestParam(value = "rideId", required = false) Long rideId,
            @RequestParam("driverId") Long driverId,
            @RequestParam("reason") String reason,
            @RequestParam("media") MultipartFile media) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }
        if (reason == null || reason.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Please enter a reason for reporting."));
        }
        if (media == null || media.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Compulsory evidence media must be attached."));
        }
        try {
            String mediaUrl = cloudinaryService.uploadImage(media);
            com.project.ridemesh.dto.DriverReportSubmitDto submitDto =
                    new com.project.ridemesh.dto.DriverReportSubmitDto(rideId, driverId, reason.trim(), mediaUrl);
            gatewayClient.submitDriverReport(token, submitDto).block();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Your report was recieved, we will look into this matter. thanks for reporting, this helps keep our plaatform safe"
            ));
        } catch (Exception e) {
            log.error("Failed to submit driver report", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/admin/reports/{reportId}/send-notice")
    public String sendReportNoticeAdmin(
            @CookieValue(value = "jwt_token", required = false) String token,
            @PathVariable Long reportId,
            @RequestParam("noticeMessage") String noticeMessage,
            RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/admin-login";
        }
        try {
            gatewayClient.sendReportNotice(token, reportId, noticeMessage).block();
            redirectAttributes.addFlashAttribute("successMessage", "Notice dispatched to driver successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to send notice: " + e.getMessage());
        }
        return "redirect:/admin/reports";
    }

    @PostMapping("/admin/reports/{reportId}/suspend-driver")
    public String suspendDriverFromReportAdmin(
            @CookieValue(value = "jwt_token", required = false) String token,
            @PathVariable Long reportId,
            RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/admin-login";
        }
        try {
            gatewayClient.suspendDriverFromReport(token, reportId).block();
            redirectAttributes.addFlashAttribute("successMessage", "Driver suspended successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to suspend driver: " + e.getMessage());
        }
        return "redirect:/admin/reports";
    }

    @PostMapping("/feedback/submit")
    @ResponseBody
    public ResponseEntity<?> submitFeedback(
            @CookieValue(value = "jwt_token", required = false) String token,
            @RequestParam("type") String type,
            @RequestParam(value = "subType", required = false) String subType,
            @RequestParam("content") String content,
            @RequestParam(value = "media", required = false) MultipartFile media) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Please enter the content."));
        }
        String normalizedType = type != null ? type.trim().toUpperCase() : "";
        if (("BUG_REPORT".equals(normalizedType) || "REPORT_BUG".equals(normalizedType)) &&
                (media == null || media.isEmpty())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Compulsory evidence media is required for Bug Reports."));
        }

        try {
            String mediaUrl = null;
            if (media != null && !media.isEmpty()) {
                mediaUrl = cloudinaryService.uploadImage(media);
            }
            com.project.ridemesh.dto.UserFeedbackSubmitDto submitDto =
                    new com.project.ridemesh.dto.UserFeedbackSubmitDto(normalizedType, subType, content.trim(), mediaUrl);
            gatewayClient.submitUserFeedback(token, submitDto).block();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Thank you! Your submission has been received by our platform team."
            ));
        } catch (Exception e) {
            log.error("Failed to submit feedback", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/admin/improvement-console")
    public String showImprovementConsole(
            @CookieValue(value = "jwt_token", required = false) String token,
            @RequestParam(value = "type", required = false) String type,
            Model model) {
        if (token == null || token.isEmpty()) {
            return "redirect:/admin-login";
        }
        try {
            Map<String, Object> claims = parseJwtPayload(token);
            String roleStr = (String) claims.get("role");
            if (roleStr == null || !roleStr.contains("ADMIN")) {
                return "redirect:/error/404";
            }
            UserDto adminUser = new UserDto(999L, "System Admin", "admin@ridemesh.com", java.util.Set.of("ADMIN"), true);
            model.addAttribute("currentUser", adminUser);

            List<com.project.ridemesh.dto.UserFeedbackDto> feedbackList =
                    gatewayClient.getUserFeedbackList(token, type).block();
            model.addAttribute("feedbackList", feedbackList != null ? feedbackList : java.util.Collections.emptyList());
            model.addAttribute("selectedType", type != null ? type.toUpperCase() : "ALL");
        } catch (Exception e) {
            log.error("Failed to load improvement console", e);
            model.addAttribute("feedbackList", java.util.Collections.emptyList());
            model.addAttribute("selectedType", "ALL");
            model.addAttribute("errorMessage", "Failed to load Improvement Console: " + e.getMessage());
        }
        return "admin/improvement-console";
    }

    @PostMapping("/admin/improvement-console/{id}/reply")
    public String replyToImprovementFeedback(
            @CookieValue(value = "jwt_token", required = false) String token,
            @PathVariable Long id,
            @RequestParam("reply") String reply,
            RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/admin-login";
        }
        try {
            gatewayClient.replyUserFeedback(token, id, reply).block();
            redirectAttributes.addFlashAttribute("successMessage", "Reply sent to user's email successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to send reply: " + e.getMessage());
        }
        return "redirect:/admin/improvement-console";
    }

    @PostMapping("/rider/completeRide/{rideId}")
    public String completeRideRider(@CookieValue(value = "jwt_token", required = false) String token,
                                    @PathVariable Long rideId,
                                    jakarta.servlet.http.HttpSession session,
                                    org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        try {
            RideDto ride = gatewayClient.completeRideRider(token, rideId).block();
            if (ride != null && ride.getStatus() == com.project.ridemesh.entities.enums.RideStatus.PAYMENT_COMPLETED) {
                session.removeAttribute("activeRequest");
                redirectAttributes.addFlashAttribute("successMessage", "Ride completed and payment processed successfully!");
                return "redirect:/rider/history";
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Confirmed completion from your side. Waiting for driver confirmation.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to confirm completion: " + e.getMessage());
        }
        return "redirect:/rider/dashboard";
    }

    @PostMapping("/driver/completeRide")
    public String completeRideDriver(@CookieValue(value = "jwt_token", required = false) String token,
                                     @RequestParam Long rideId,
                                     jakarta.servlet.http.HttpSession session,
                                     org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        try {
            RideDto ride = gatewayClient.completeRideDriver(token, rideId).block();
            if (ride != null && ride.getStatus() == com.project.ridemesh.entities.enums.RideStatus.PAYMENT_COMPLETED) {
                session.removeAttribute("activeDriverRide");
                redirectAttributes.addFlashAttribute("successMessage", "Ride completed and payment processed successfully!");
                return "redirect:/driver/history";
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Confirmed completion from your side. Waiting for rider confirmation.");
            }
        } catch (Exception e) {
            session.setAttribute("driverError", "Failed to complete ride: " + e.getMessage());
        }
        return "redirect:/driver/dashboard";
    }

    @GetMapping("/rider/checkStatus")
    @ResponseBody
    public Map<String, Object> checkRiderStatus(@CookieValue(value = "jwt_token", required = false) String token, jakarta.servlet.http.HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (token == null || token.isEmpty()) {
            result.put("status", "UNAUTHENTICATED");
            result.put("hasActiveRide", false);
            return result;
        }
        try {
            RideDto activeRide = gatewayClient.getActiveRideRider(token).block();
            if (activeRide != null) {
                session.removeAttribute("activeRequest");
                result.put("hasActiveRide", true);
                result.put("rideStatus", activeRide.getStatus() != null ? activeRide.getStatus().name() : "");
                result.put("rideId", activeRide.getId());
                return result;
            }
        } catch (Exception ignored) {}
        result.put("hasActiveRide", false);
        return result;
    }

    @GetMapping("/driver/checkStatus")
    @ResponseBody
    public Map<String, Object> checkDriverStatus(@CookieValue(value = "jwt_token", required = false) String token) {
        Map<String, Object> result = new HashMap<>();
        if (token == null || token.isEmpty()) {
            result.put("status", "UNAUTHENTICATED");
            result.put("hasActiveRide", false);
            return result;
        }
        try {
            RideDto activeRide = gatewayClient.getActiveRideDriver(token).block();
            boolean hasActive = (activeRide != null);
            result.put("hasActiveRide", hasActive);
            if (activeRide != null) {
                result.put("rideStatus", activeRide.getStatus() != null ? activeRide.getStatus().name() : "");
                result.put("rideId", activeRide.getId());
            } else {
                try {
                    List<RideRequestDto> pending = gatewayClient.getPendingRidesNearDriver(token).block();
                    result.put("pendingCount", pending != null ? pending.size() : 0);
                } catch (Exception e) {
                    result.put("pendingCount", 0);
                }
            }
        } catch (Exception e) {
            result.put("hasActiveRide", false);
        }
        return result;
    }

    private Map<String, Object> parseJwtPayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length > 1) {
                String decoded = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(decoded, Map.class);
            }
        } catch (Exception e) {
            // Ignore
        }
        return java.util.Collections.emptyMap();
    }
}
