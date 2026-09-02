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

@Service
public class AuthServiceImpl implements AuthService {

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
        Optional<User> user = userRepository.findByEmail(signupDto.getEmail());
        if(user.isPresent()){
            throw new RuntimeConflictException("User already exists with that email!");
        }
        User mappedUser = modelMapper.map(signupDto, User.class);
        mappedUser.setPassword(passwordEncoder.encode(mappedUser.getPassword()));
        mappedUser.setRoles(Set.of(Roles.RIDER));
        User savedUser = userRepository.save(mappedUser);
        riderService.createNewRider(savedUser);

        // Publish event to Kafka to initialize the Wallet asynchronously
        kafkaTemplate.send("user-signup-event", String.valueOf(savedUser.getId()));

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
        kafkaTemplate.send("send-email-verification", payload);
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
}
