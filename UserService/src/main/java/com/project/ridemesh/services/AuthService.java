package com.project.ridemesh.services;

import com.project.ridemesh.dto.*;

public interface AuthService {
    String[] login(LoginRequestDto loginRequestDto);
    UserDto signup(SignupDto signupDto);
    DriverDto onboardNewDriver(Long userId, OnboardDriverDto onboardDriverDto);
    LoginResponseDto refreshToken(String refreshToken);
    void sendVerificationEmail(Long userId);
    void verifyEmail(String token);
    String[] loginOrSignupWithGoogle(GoogleAuthDto googleAuthDto);
    boolean checkUsernameExists(String username);
}
