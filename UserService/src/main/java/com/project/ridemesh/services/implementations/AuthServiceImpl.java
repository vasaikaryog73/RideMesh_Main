package com.project.ridemesh.services.implementations;

import com.project.ridemesh.dto.*;
import com.project.ridemesh.entities.Driver;
import com.project.ridemesh.entities.User;
import com.project.ridemesh.entities.enums.Roles;
import com.project.ridemesh.exceptions.ResourceNotFoundException;
import com.project.ridemesh.exceptions.RuntimeConflictException;
import com.project.ridemesh.repositories.UserRepository;
import com.project.ridemesh.security.JwtService;
import com.project.ridemesh.services.*;
import com.project.ridemesh.utils.GeometryUtil;
import org.locationtech.jts.geom.Point;
import org.modelmapper.ModelMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import com.project.ridemesh.entities.VerificationToken;
import com.project.ridemesh.repositories.VerificationTokenRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final RiderService riderService;
    private final DriverService driverService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final VerificationTokenRepository verificationTokenRepository;

    public AuthServiceImpl(ModelMapper modelMapper, AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder, UserService userService, JwtService jwtService, UserRepository userRepository, DriverService driverService, RiderService riderService, KafkaTemplate<String, String> kafkaTemplate, VerificationTokenRepository verificationTokenRepository) {
        this.modelMapper = modelMapper;
        this.userRepository = userRepository;
        this.riderService = riderService;
        this.driverService = driverService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
        this.kafkaTemplate = kafkaTemplate;
        this.verificationTokenRepository = verificationTokenRepository;
    }

    @Override
    public String[] login(LoginRequestDto loginRequestDto) {
        String email = loginRequestDto.getEmail() != null ? loginRequestDto.getEmail().trim().toLowerCase() : "";
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent() && existingUser.get().getAuthProvider() == com.project.ridemesh.entities.enums.AuthProvider.GOOGLE) {
            throw new RuntimeConflictException("Different authentication method in use for this email");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequestDto.getEmail(), loginRequestDto.getPassword())
        );

        User user = (User) authentication.getPrincipal();

        String accessToken = jwtService.getAccessJwtToken(user);
        String refreshToken = jwtService.getRefreshJwtToken(user);

        return new String[]{accessToken, refreshToken};
    }

    @Override
    @Transactional
    public UserDto signup(SignupDto signupDto) {
        String email = signupDto.getEmail() != null ? signupDto.getEmail().trim().toLowerCase() : "";
        Optional<User> user = userRepository.findByEmail(email);
        if(user.isPresent()){
            if (user.get().getAuthProvider() == com.project.ridemesh.entities.enums.AuthProvider.GOOGLE) {
                throw new RuntimeConflictException("Different authentication method in use for this email");
            }
            throw new RuntimeConflictException("User already exists with that email!");
        }

        String username = signupDto.getUsername();
        if (username == null || !username.matches("^[a-zA-Z]{5}#[0-9]{5}$")) {
            throw new RuntimeConflictException("Username must consist of 5 letters followed by # and 5 numbers (e.g. abcde#12345)");
        }
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeConflictException("Username is already taken!");
        }

        User mappedUser = modelMapper.map(signupDto, User.class);
        mappedUser.setPassword(passwordEncoder.encode(mappedUser.getPassword()));
        mappedUser.setRoles(Set.of(Roles.RIDER));
        mappedUser.setUsername(username);
        mappedUser.setAuthProvider(com.project.ridemesh.entities.enums.AuthProvider.EMAIL);
        if (mappedUser.getProfilePictureUrl() == null || mappedUser.getProfilePictureUrl().trim().isEmpty()) {
            mappedUser.setProfilePictureUrl("https://res.cloudinary.com/domckasfk/image/upload/v1774786542/mediscan/reports/ygnjdwtvacvw6rrzmumc.jpg");
        }
        if (signupDto.getPhoneNumber() != null && !signupDto.getPhoneNumber().trim().isEmpty()) {
            mappedUser.setPhoneNumber(signupDto.getPhoneNumber().trim());
        }
        User savedUser = userRepository.save(mappedUser);
        riderService.createNewRider(savedUser);

        // Publish event to Kafka to initialize the Wallet asynchronously
        try {
            kafkaTemplate.send("user-signup-event", String.valueOf(savedUser.getId()));
        } catch (Exception e) {
            log.warn("Kafka unavailable: could not publish user-signup-event for user ID {}: {}", savedUser.getId(), e.getMessage());
        }

        return modelMapper.map(savedUser, UserDto.class);
    }

    @Override
    @Transactional
    public DriverDto onboardNewDriver(Long userId, OnboardDriverDto onboardDriverDto) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(()-> new ResourceNotFoundException("No user was found with ID: "+userId));

        if(user.getRoles().contains(Roles.DRIVER)){
            throw new RuntimeConflictException("User is already a driver!");
        }

        Point currentLocation = GeometryUtil.createPoint(onboardDriverDto.getCurrentLocation());
        Driver createDriver = new Driver
                .DriverBuilder()
                .available(true)
                .rating(0.0)
                .user(user)
                .vehicleId(onboardDriverDto.getVehicleId())
                .currentLocation(currentLocation)
                .vehicleType(onboardDriverDto.getVehicleType())
                .phoneNumber(onboardDriverDto.getPhoneNumber())
                .state(onboardDriverDto.getState())
                .district(onboardDriverDto.getDistrict())
                .build();

        user.getRoles().add(Roles.DRIVER);
        userRepository.save(user);
        Driver savedDriver = driverService.createNewDriver(createDriver);

        return modelMapper.map(savedDriver, DriverDto.class);
    }

    @Override
    public LoginResponseDto refreshToken(String refreshToken) {
        Long userId = jwtService.getUserId(refreshToken);
        User user = userService.getUserFromId(userId);
        if(user == null) throw new AuthenticationCredentialsNotFoundException("User not found");

        String accessToken = jwtService.getAccessJwtToken(user);
        return new LoginResponseDto(accessToken);
    }

    @Override
    @Transactional
    public void sendVerificationEmail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new RuntimeConflictException("Email is already verified!");
        }

        // Delete any existing tokens for this user first (prev link expires)
        verificationTokenRepository.deleteByUser(user);

        // Generate new token
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, user);
        verificationTokenRepository.save(verificationToken);

        // Publish send-email-verification event to Kafka
        String verificationLink = "http://localhost:8085/verify?token=" + token;
        String payload = String.format("{\"email\":\"%s\",\"verificationLink\":\"%s\"}", user.getEmail(), verificationLink);
        try {
            kafkaTemplate.send("send-email-verification", payload);
        } catch (Exception e) {
            log.warn("Kafka unavailable: could not publish send-email-verification: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or missing verification token!"));

        if (verificationToken.isExpired()) {
            verificationTokenRepository.delete(verificationToken);
            throw new RuntimeConflictException("Verification token has expired!");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        // Clean up verification token
        verificationTokenRepository.delete(verificationToken);
    }

    @Override
    @Transactional
    public String[] loginOrSignupWithGoogle(GoogleAuthDto googleAuthDto) {
        String email = googleAuthDto.getEmail();
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email from Google account cannot be empty");
        }

        User user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);

        if (user != null) {
            if (user.getAuthProvider() == com.project.ridemesh.entities.enums.AuthProvider.EMAIL) {
                throw new RuntimeConflictException("Different authentication method in use for this email");
            }
            boolean updated = false;
            if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                user.setEmailVerified(true);
                updated = true;
            }
            if (googleAuthDto.getProfilePictureUrl() != null && !googleAuthDto.getProfilePictureUrl().trim().isEmpty()) {
                user.setProfilePictureUrl(googleAuthDto.getProfilePictureUrl().trim());
                updated = true;
            }
            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                String rawLetters = user.getName().replaceAll("[^a-zA-Z]", "");
                if (rawLetters.length() < 5) {
                    rawLetters = rawLetters + "abcde";
                }
                String prefix = rawLetters.substring(0, 5);
                int randNum = new java.util.Random().nextInt(100000);
                String candidateUsername = String.format("%s#%05d", prefix, randNum);
                while (userRepository.existsByUsername(candidateUsername)) {
                    randNum = new java.util.Random().nextInt(100000);
                    candidateUsername = String.format("%s#%05d", prefix, randNum);
                }
                user.setUsername(candidateUsername);
                updated = true;
            }
            if (updated) {
                user = userRepository.save(user);
            }
        } else {
            user = new User();
            String name = googleAuthDto.getName();
            user.setName((name != null && !name.trim().isEmpty()) ? name.trim() : "RideMesh User");
            user.setEmail(email.trim().toLowerCase());
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setEmailVerified(true);
            user.setRoles(Set.of(Roles.RIDER));
            user.setAuthProvider(com.project.ridemesh.entities.enums.AuthProvider.GOOGLE);

            // Generate unique username: 5 letters from google name + '#' + 5 random digits
            String rawLetters = user.getName().replaceAll("[^a-zA-Z]", "");
            if (rawLetters.length() < 5) {
                String emailLetters = email.split("@")[0].replaceAll("[^a-zA-Z]", "");
                rawLetters = rawLetters + emailLetters + "abcde";
            }
            String prefix = rawLetters.substring(0, 5);
            int randNum = new java.util.Random().nextInt(100000);
            String candidateUsername = String.format("%s#%05d", prefix, randNum);
            while (userRepository.existsByUsername(candidateUsername)) {
                randNum = new java.util.Random().nextInt(100000);
                candidateUsername = String.format("%s#%05d", prefix, randNum);
            }
            user.setUsername(candidateUsername);

            // Set profile picture from Google (stored in Cloudinary) or default
            String pfp = googleAuthDto.getProfilePictureUrl();
            if (pfp != null && !pfp.trim().isEmpty()) {
                user.setProfilePictureUrl(pfp.trim());
            } else {
                user.setProfilePictureUrl("https://res.cloudinary.com/domckasfk/image/upload/v1774786542/mediscan/reports/ygnjdwtvacvw6rrzmumc.jpg");
            }

            user = userRepository.save(user);
            riderService.createNewRider(user);

            // Publish event to Kafka to initialize the Wallet asynchronously
            try {
                kafkaTemplate.send("user-signup-event", String.valueOf(user.getId()));
            } catch (Exception e) {
                log.warn("Kafka unavailable: could not publish user-signup-event for user ID {}: {}", user.getId(), e.getMessage());
            }
        }

        String accessToken = jwtService.getAccessJwtToken(user);
        String refreshToken = jwtService.getRefreshJwtToken(user);

        return new String[]{accessToken, refreshToken};
    }

    @Override
    public boolean checkUsernameExists(String username) {
        if (username == null || username.trim().isEmpty()) return false;
        return userRepository.existsByUsername(username.trim());
    }
}
