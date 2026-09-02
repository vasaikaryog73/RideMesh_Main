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
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import com.project.ridemesh.dto.DriverDto;

@Controller
public class AuthUiController {

    private final GatewayClient gatewayClient;
    private final com.project.ridemesh.client.CloudinaryService cloudinaryService;

    public AuthUiController(GatewayClient gatewayClient, com.project.ridemesh.client.CloudinaryService cloudinaryService) {
        this.gatewayClient = gatewayClient;
        this.cloudinaryService = cloudinaryService;
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
    public String loginPage(Model model, @CookieValue(value = "jwt_token", required = false) String token) {
        if (token != null && !token.isEmpty()) {
            return "redirect:/dashboard";
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
        } catch (Exception e) {
            model.addAttribute("error", "Invalid email or password. Please try again.");
        }
        model.addAttribute("loginRequest", loginRequest);
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signupPage(Model model) {
        model.addAttribute("signupRequest", new SignupDto());
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String processSignup(@ModelAttribute SignupDto signupRequest, 
                                @RequestParam String confirmPassword, 
                                Model model) {
        if (!signupRequest.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("signupRequest", signupRequest);
            return "auth/signup";
        }
        try {
            gatewayClient.signup(signupRequest).block();
            return "redirect:/login?signupSuccess=true";
        } catch (Exception e) {
            model.addAttribute("error", "Email registration failed. It may already be in use.");
        }
        model.addAttribute("signupRequest", signupRequest);
        return "auth/signup";
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
                              jakarta.servlet.http.HttpSession session) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        session.setAttribute("bookingError", "Sorry, this feature is yet to be implemented");
        return "redirect:/rider/dashboard";
    }

    @PostMapping("/rider/cancelRequest")
    public String cancelActiveRequest(jakarta.servlet.http.HttpSession session) {
        session.removeAttribute("activeRequest");
        session.removeAttribute("bookingError");
        return "redirect:/rider/dashboard";
    }

    @GetMapping("/driver/dashboard")
    public String driverDashboard(@CookieValue(value = "jwt_token", required = false) String token, 
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
            model.addAttribute("currentUser", profile);
            model.addAttribute("activeRide", null);
            model.addAttribute("pendingRides", java.util.Collections.emptyList());

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
                             jakarta.servlet.http.HttpSession session) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        session.setAttribute("driverError", "Sorry, this feature is yet to be implemented");
        return "redirect:/driver/dashboard";
    }

    @PostMapping("/driver/startRide")
    public String startRide(@CookieValue(value = "jwt_token", required = false) String token,
                            jakarta.servlet.http.HttpSession session) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        session.setAttribute("driverError", "Sorry, this feature is yet to be implemented");
        return "redirect:/driver/dashboard";
    }

    @PostMapping("/driver/endRide")
    public String endRide(@CookieValue(value = "jwt_token", required = false) String token,
                          jakarta.servlet.http.HttpSession session) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        session.setAttribute("driverError", "Sorry, this feature is yet to be implemented");
        return "redirect:/driver/dashboard";
    }

    @PostMapping("/driver/cancelRide")
    public String cancelDriverRide(@CookieValue(value = "jwt_token", required = false) String token,
                                   jakarta.servlet.http.HttpSession session) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        session.setAttribute("driverError", "Sorry, this feature is yet to be implemented");
        return "redirect:/driver/dashboard";
    }

    @PostMapping("/driver/rateRider/{rideId}")
    public String rateRider(@CookieValue(value = "jwt_token", required = false) String token,
                            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("errorMessage", "Sorry, this feature is yet to be implemented");
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
            model.addAttribute("wallet", wallet != null ? wallet : new WalletDto());
        } catch (Exception e) {
            WalletDto emptyWallet = new WalletDto();
            emptyWallet.setBalance(0.0);
            emptyWallet.setTransactions(java.util.Collections.emptyList());
            model.addAttribute("wallet", emptyWallet);
        }
        return "rider/wallet";
    }

    @PostMapping("/rider/wallet/create-order")
    @ResponseBody
    public ResponseEntity<?> createOrder(@CookieValue(value = "jwt_token", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Sorry, this feature is yet to be implemented"));
    }

    @PostMapping("/rider/wallet/verify-payment")
    @ResponseBody
    public ResponseEntity<?> verifyPayment(@CookieValue(value = "jwt_token", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Sorry, this feature is yet to be implemented"));
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
                                org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/admin-login";
        }
        redirectAttributes.addFlashAttribute("errorMessage", "Sorry, this feature is yet to be implemented");
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/rider/cancelRide/{rideId}")
    public String cancelConfirmedRide(@CookieValue(value = "jwt_token", required = false) String token,
                                      @PathVariable Long rideId,
                                      org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("errorMessage", "Sorry, this feature is yet to be implemented");
        return "redirect:/rider/history";
    }

    @PostMapping("/rider/rateDriver/{rideId}")
    public String rateDriver(@CookieValue(value = "jwt_token", required = false) String token,
                             @PathVariable Long rideId,
                             org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("errorMessage", "Sorry, this feature is yet to be implemented");
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
        } catch (Exception e) {
            return "redirect:/logout";
        }
        return "rider/become-driver";
    }

    @PostMapping("/rider/become-driver")
    public String processBecomeDriver(@CookieValue(value = "jwt_token", required = false) String token,
                                      org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("errorMessage", "Sorry, this feature is yet to be implemented");
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
            model.addAttribute("applications", java.util.Collections.emptyList());
            
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
        redirectAttributes.addFlashAttribute("errorMessage", "Sorry, this feature is yet to be implemented");
        return "redirect:/admin/applications";
    }

    @PostMapping("/admin/applications/{id}/reject")
    public String rejectApplication(@CookieValue(value = "jwt_token", required = false) String token,
                                     @PathVariable Long id,
                                     org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/admin-login";
        }
        redirectAttributes.addFlashAttribute("errorMessage", "Sorry, this feature is yet to be implemented");
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
            model.addAttribute("drivers", java.util.Collections.emptyList());

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
        redirectAttributes.addFlashAttribute("errorMessage", "Sorry, this feature is yet to be implemented");
        return "redirect:/admin/drivers";
    }

    @PostMapping("/rider/completeRide/{rideId}")
    public String completeRideRider(@CookieValue(value = "jwt_token", required = false) String token,
                                    @PathVariable Long rideId,
                                    org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("errorMessage", "Sorry, this feature is yet to be implemented");
        return "redirect:/rider/dashboard";
    }

    @PostMapping("/driver/completeRide")
    public String completeRideDriver(@CookieValue(value = "jwt_token", required = false) String token,
                                     jakarta.servlet.http.HttpSession session) {
        if (token == null || token.isEmpty()) {
            return "redirect:/login";
        }
        session.setAttribute("driverError", "Sorry, this feature is yet to be implemented");
        return "redirect:/driver/dashboard";
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
