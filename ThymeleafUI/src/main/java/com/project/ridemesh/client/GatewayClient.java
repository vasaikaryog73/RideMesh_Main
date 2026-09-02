package com.project.ridemesh.client;

import com.project.ridemesh.dto.ApiResponse;
import com.project.ridemesh.dto.LoginRequestDto;
import com.project.ridemesh.dto.LoginResponseDto;
import com.project.ridemesh.dto.SignupDto;
import com.project.ridemesh.dto.UserDto;
import com.project.ridemesh.dto.RiderDto;
import com.project.ridemesh.dto.DriverDto;
import com.project.ridemesh.dto.WalletDto;
import com.project.ridemesh.dto.RideDto;
import com.project.ridemesh.dto.PageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Map;
import java.util.List;

import com.project.ridemesh.dto.RideRequestDto;

@Component
public class GatewayClient {

    private final WebClient webClient;

    public GatewayClient(@Value("${gateway.base-url}") String gatewayUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(gatewayUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<LoginResponseDto> login(LoginRequestDto requestDto) {
        return webClient.post()
                .uri("/auth/login")
                .bodyValue(requestDto)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<LoginResponseDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<UserDto> signup(SignupDto signupDto) {
        return webClient.post()
                .uri("/auth/signup")
                .bodyValue(signupDto)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<UserDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<UserDto> getProfile(String token) {
        return webClient.get()
                .uri("/rider/getMyProfile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<RiderDto>>() {})
                .map(ApiResponse::getData)
                .map(RiderDto::getUser)
                .onErrorResume(e -> webClient.get()
                        .uri("/driver/getMyProfile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<ApiResponse<DriverDto>>() {})
                        .map(ApiResponse::getData)
                        .map(DriverDto::getUser));
    }

    public Mono<Void> sendVerification(String token) {
        return webClient.post()
                .uri("/auth/send-verification")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<Void> verifyEmail(String verificationToken) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/auth/verify-email")
                        .queryParam("token", verificationToken)
                        .build())
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<WalletDto> getWalletBalance(String token) {
        return webClient.get()
                .uri("/wallet/balance")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(WalletDto.class);
    }

    public Mono<Map<String, Object>> createRazorpayOrder(String token, Double amount) {
        return webClient.post()
                .uri("/wallet/create-order")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(Map.of("amount", amount))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    public Mono<WalletDto> verifyRazorpayPayment(String token, Map<String, String> payload) {
        return webClient.post()
                .uri("/wallet/verify-payment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(WalletDto.class);
    }

    public Mono<PageResponse<RideDto>> getRiderRides(String token, int page, int size) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rider/getMyRides")
                        .queryParam("pageOffset", page)
                        .queryParam("pageSize", size)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<PageResponse<RideDto>>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<PageResponse<RideDto>> getDriverRides(String token, int page, int size) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/driver/getMyRides")
                        .queryParam("pageOffset", page)
                        .queryParam("pageSize", size)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<PageResponse<RideDto>>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<RideRequestDto> requestRide(String token, RideRequestDto requestDto) {
        return webClient.post()
                .uri("/rider/requestRide")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(requestDto)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<RideRequestDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<RideDto> cancelRide(String token, Long rideId) {
        return webClient.post()
                .uri("/rider/cancelRide/" + rideId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<RideDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<DriverDto> onboardDriver(String token, Long userId, Long vehicleId) {
        Map<String, Object> req = Map.of(
            "vehicleId", vehicleId,
            "currentLocation", Map.of("coordinates", new double[]{72.8777, 19.0760})
        );
        return webClient.post()
                .uri("/auth/onboardDriver/" + userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<DriverDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<DriverDto> rateDriver(String token, Long rideId, Double rating) {
        return webClient.post()
                .uri("/rider/rateDriver/" + rideId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(Map.of("rating", rating))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<DriverDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<RideDto> acceptRide(String token, Long rideRequestId) {
        return webClient.post()
                .uri("/driver/acceptRide/" + rideRequestId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<RideDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<RideDto> startRide(String token, Long rideId, String otp) {
        return webClient.post()
                .uri("/driver/startRide/" + rideId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(new com.project.ridemesh.dto.RideStartDto(otp))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<RideDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<RideDto> endRide(String token, Long rideId) {
        return webClient.post()
                .uri("/driver/endRide/" + rideId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<RideDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<RideDto> cancelDriverRide(String token, Long rideId) {
        return webClient.post()
                .uri("/driver/cancelRide/" + rideId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<RideDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<RiderDto> rateRider(String token, Long rideId, Double rating) {
        return webClient.post()
                .uri("/driver/rateRider/" + rideId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(Map.of("rating", rating))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<RiderDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<com.project.ridemesh.dto.DriverApplicationDto> submitDriverApplication(String token, com.project.ridemesh.dto.DriverApplicationDto dto) {
        return webClient.post()
                .uri("/auth/driver-applications")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<com.project.ridemesh.dto.DriverApplicationDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<List<com.project.ridemesh.dto.DriverApplicationDto>> getDriverApplications(String token) {
        return webClient.get()
                .uri("/auth/driver-applications")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<com.project.ridemesh.dto.DriverApplicationDto>>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<com.project.ridemesh.dto.DriverApplicationDto> approveDriverApplication(String token, Long id) {
        return webClient.post()
                .uri("/auth/driver-applications/" + id + "/approve")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<com.project.ridemesh.dto.DriverApplicationDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<com.project.ridemesh.dto.DriverApplicationDto> rejectDriverApplication(String token, Long id) {
        return webClient.post()
                .uri("/auth/driver-applications/" + id + "/reject")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<com.project.ridemesh.dto.DriverApplicationDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<List<DriverDto>> getActiveDrivers(String token) {
        return webClient.get()
                .uri("/auth/drivers")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<DriverDto>>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<Void> suspendDriver(String token, Long userId) {
        return webClient.post()
                .uri("/auth/drivers/" + userId + "/suspend")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<List<com.project.ridemesh.dto.RideRequestDto>> getPendingRidesNearDriver(String token) {
        return webClient.get()
                .uri("/driver/pendingRides")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<com.project.ridemesh.dto.RideRequestDto>>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<RideDto> completeRideDriver(String token, Long rideId) {
        return webClient.post()
                .uri("/driver/completeRide/" + rideId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<RideDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<RideDto> completeRideRider(String token, Long rideId) {
        return webClient.post()
                .uri("/rider/completeRide/" + rideId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<RideDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<RideDto> getActiveRideRider(String token) {
        return webClient.get()
                .uri("/rider/activeRide")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<RideDto>>() {})
                .map(ApiResponse::getData)
                .onErrorReturn(null);
    }

    public Mono<RideDto> getActiveRideDriver(String token) {
        return webClient.get()
                .uri("/driver/activeRide")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<RideDto>>() {})
                .map(ApiResponse::getData)
                .onErrorReturn(null);
    }
}
