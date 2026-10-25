package com.project.ridemesh.controllers.auth;

import com.project.ridemesh.dto.*;
import com.project.ridemesh.services.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/auth")
public class AuthPostMapping {

    @Value("${deploy.env}")
    private String deployment;
    private final AuthService authService;
    private final com.project.ridemesh.services.UserService userService;
    private final com.project.ridemesh.security.JwtService jwtService;

    public AuthPostMapping(AuthService authService, com.project.ridemesh.services.UserService userService, com.project.ridemesh.security.JwtService jwtService) {
        this.authService = authService;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping(path = "/refresh-token/{userId}")
    public ResponseEntity<LoginResponseDto> refreshTokenForUser(@PathVariable Long userId) {
        com.project.ridemesh.entities.User user = userService.getUserFromId(userId);
        String accessToken = jwtService.getAccessJwtToken(user);
        return ResponseEntity.ok(new LoginResponseDto(accessToken));
    }

    @PostMapping(path = "/signup")
    public ResponseEntity<UserDto> signup(@RequestBody SignupDto signupDto){
        UserDto user = authService.signup(signupDto);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @PostMapping(path = "/onboardDriver/{userId}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<DriverDto> onboardDriver(@PathVariable Long userId, @RequestBody OnboardDriverDto onboardDriverDto){
        DriverDto driver = authService.onboardNewDriver(userId, onboardDriverDto);
        return new ResponseEntity<>(driver, HttpStatus.CREATED);
    }

    @PostMapping(path = "/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequestDto, HttpServletResponse httpServletResponse){
        String[] tokens = authService.login(loginRequestDto);
        Cookie cookie = new Cookie("refreshToken", tokens[1]);
        cookie.setHttpOnly(true);
        cookie.setSecure(deployment.equals("production"));
        cookie.setPath("/auth/refresh");
        httpServletResponse.addCookie(cookie);

        return ResponseEntity.ok(new LoginResponseDto(tokens[0]));
    }

    @PostMapping(path = "/refresh")
    public ResponseEntity<LoginResponseDto> refreshToken(HttpServletRequest httpServletRequest){
        if (httpServletRequest.getCookies() == null) {
            throw new AuthenticationCredentialsNotFoundException("Refresh token cookie not found");
        }
        String refreshToken = Arrays
                .stream(httpServletRequest
                        .getCookies())
                        .filter(cookie -> cookie.getName().equals("refreshToken"))
                        .findFirst()
                        .map(Cookie::getValue)
                        .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Refresh token not found"));
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    @PostMapping(path = "/send-verification")
    public ResponseEntity<Void> sendVerification(@RequestHeader("X-User-Id") Long userId) {
        authService.sendVerificationEmail(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping(path = "/google")
    public ResponseEntity<LoginResponseDto> googleAuth(@RequestBody GoogleAuthDto googleAuthDto, HttpServletResponse httpServletResponse){
        String[] tokens = authService.loginOrSignupWithGoogle(googleAuthDto);
        Cookie cookie = new Cookie("refreshToken", tokens[1]);
        cookie.setHttpOnly(true);
        cookie.setSecure(deployment.equals("production"));
        cookie.setPath("/auth/refresh");
        httpServletResponse.addCookie(cookie);

        return ResponseEntity.ok(new LoginResponseDto(tokens[0]));
    }

    @GetMapping(path = "/check-username")
    public ResponseEntity<Boolean> checkUsername(@RequestParam String username) {
        return ResponseEntity.ok(authService.checkUsernameExists(username));
    }

    @PutMapping(path = "/profile")
    public ResponseEntity<UserDto> updateProfile(@RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
                                                 @RequestBody UpdateProfileDto updateProfileDto) {
        Long userId = headerUserId;
        if (userId == null) {
            Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof com.project.ridemesh.entities.User) {
                userId = ((com.project.ridemesh.entities.User) principal).getId();
            }
        }
        if (userId == null) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("User not authenticated");
        }
        return ResponseEntity.ok(userService.updateProfile(userId, updateProfileDto));
    }
}
