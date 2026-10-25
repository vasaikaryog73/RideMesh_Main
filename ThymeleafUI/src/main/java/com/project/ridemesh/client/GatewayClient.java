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
import java.util.HashMap;
import java.util.List;

import com.project.ridemesh.dto.RideRequestDto;

import com.project.ridemesh.dto.GoogleAuthDto;

@Component
public class GatewayClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GatewayClient.class);

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

    public Mono<LoginResponseDto> googleAuth(GoogleAuthDto authDto) {
        return webClient.post()
                .uri("/auth/google")
                .bodyValue(authDto)
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

    public Mono<Boolean> checkUsernameExists(String username) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/auth/check-username")
                        .queryParam("username", username)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<Boolean>>() {})
                .map(ApiResponse::getData)
                .onErrorReturn(false);
    }

    public Mono<UserDto> updateProfile(String token, String name, String profilePictureUrl) {
        return updateProfile(token, name, profilePictureUrl, null);
    }

    public Mono<UserDto> updateProfile(String token, String name, String profilePictureUrl, String phoneNumber) {
        Map<String, String> body = new java.util.HashMap<>();
        body.put("name", name);
        if (profilePictureUrl != null && !profilePictureUrl.trim().isEmpty()) {
            body.put("profilePictureUrl", profilePictureUrl);
        }
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            body.put("phoneNumber", phoneNumber);
        }
        return webClient.put()
                .uri("/auth/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(body)
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

    public Mono<LoginResponseDto> refreshTokenForUser(Long userId) {
        return webClient.post()
                .uri("/auth/refresh-token/" + userId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<LoginResponseDto>>() {})
                .map(ApiResponse::getData);
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

    public Mono<Map<String, Object>> createRazorpayWithdrawOrder(String token, Double amount) {
        return webClient.post()
                .uri("/wallet/create-withdraw-order")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(Map.of("amount", amount))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    public Mono<WalletDto> verifyRazorpayWithdraw(String token, Map<String, String> payload) {
        return webClient.post()
                .uri("/wallet/verify-withdraw")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(WalletDto.class);
    }

    public Mono<Map<String, Object>> payRideUpi(String token, Map<String, Object> payload) {
        return webClient.post()
                .uri("/wallet/pay-ride-upi")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
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

    public Mono<Void> cancelRideRequest(String token, Long requestId) {
        return webClient.post()
                .uri("/rider/cancelRequest/" + requestId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Void.class);
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
        return rateDriver(token, rideId, rating, null);
    }

    public Mono<DriverDto> rateDriver(String token, Long rideId, Double rating, Long driverId) {
        Map<String, Object> body = new HashMap<>();
        body.put("rating", rating);
        if (driverId != null) {
            body.put("driverId", driverId);
        }
        return webClient.post()
                .uri("/rider/rateDriver/" + rideId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<DriverDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<RideDto> recordRideRating(String token, Long rideId, Double rating) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/rider/completeRide/" + rideId + "/rate")
                        .queryParam("rating", rating)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<RideDto>>() {})
                .map(ApiResponse::getData)
                .onErrorResume(e -> {
                    log.warn("recordRideRating error: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    public Mono<DriverDto> getDriverProfile(String token) {
        return webClient.get()
                .uri("/driver/getMyProfile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<DriverDto>>() {})
                .map(ApiResponse::getData)
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<RideDto> acceptRide(String token, Long rideRequestId) {
        return webClient.post()
                .uri("/driver/acceptRide/" + rideRequestId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<RideDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<Void> declineRide(String token, Long rideRequestId) {
        return webClient.post()
                .uri("/driver/declineRide/" + rideRequestId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<RideDto> confirmCashPaid(String token, Long rideId) {
        return webClient.post()
                .uri("/rider/confirmCashPaid/" + rideId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(RideDto.class);
    }

    public Mono<RideDto> finalizeUpiPayment(String token, Long rideId) {
        return webClient.post()
                .uri("/rider/finalizeUpi/" + rideId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(RideDto.class);
    }

    public Mono<RideDto> confirmCashReceived(String token, Long rideId) {
        return webClient.post()
                .uri("/driver/confirmCashReceived/" + rideId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(RideDto.class);
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

    public Mono<com.project.ridemesh.dto.DriverApplicationDto> getMyDriverApplication(String token) {
        return getMyDriverApplication(token, null);
    }

    public Mono<com.project.ridemesh.dto.DriverApplicationDto> getMyDriverApplication(String token, Long userId) {
        String uri = "/auth/driver-applications/my-application" + (userId != null ? "?userId=" + userId : "");
        return webClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<com.project.ridemesh.dto.DriverApplicationDto>>() {})
                .map(res -> res != null && res.getData() != null ? res.getData() : new com.project.ridemesh.dto.DriverApplicationDto())
                .doOnError(e -> log.error("Failed to fetch driver application: {}", e.getMessage()))
                .onErrorReturn(new com.project.ridemesh.dto.DriverApplicationDto());
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
                .flatMap(res -> res != null && res.getData() != null ? Mono.just(res.getData()) : Mono.just(java.util.Collections.<com.project.ridemesh.dto.RideRequestDto>emptyList()))
                .onErrorResume(e -> Mono.just(java.util.Collections.emptyList()));
    }

    public Mono<RideDto> completeRideDriver(String token, Long rideId) {
        return webClient.post()
                .uri("/driver/completeRide/" + rideId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<RideDto>>() {})
                .flatMap(res -> res != null && res.getData() != null ? Mono.just(res.getData()) : Mono.empty())
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<RideDto> completeRideRider(String token, Long rideId) {
        return webClient.post()
                .uri("/rider/completeRide/" + rideId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<RideDto>>() {})
                .flatMap(res -> res != null && res.getData() != null ? Mono.just(res.getData()) : Mono.empty())
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<RideDto> getActiveRideRider(String token) {
        return webClient.get()
                .uri("/rider/activeRide")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<RideDto>>() {})
                .flatMap(res -> res != null && res.getData() != null ? Mono.just(res.getData()) : Mono.empty())
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<RideDto> getActiveRideDriver(String token) {
        return webClient.get()
                .uri("/driver/activeRide")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<RideDto>>() {})
                .flatMap(res -> res != null && res.getData() != null ? Mono.just(res.getData()) : Mono.empty())
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<Map<String, Object>> sendCancellationOtp(String token) {
        return webClient.post()
                .uri("/auth/driver-cancellation/send-otp")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    public Mono<com.project.ridemesh.dto.DriverCancellationRequestDto> submitCancellationRequest(
            String token, com.project.ridemesh.dto.DriverCancellationSubmitDto dto) {
        return webClient.post()
                .uri("/auth/driver-cancellation/submit")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<com.project.ridemesh.dto.DriverCancellationRequestDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<List<com.project.ridemesh.dto.DriverCancellationRequestDto>> getCancellationRequests(String token) {
        return webClient.get()
                .uri("/auth/driver-cancellation/requests")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<com.project.ridemesh.dto.DriverCancellationRequestDto>>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<com.project.ridemesh.dto.DriverCancellationRequestDto> approveCancellationRequest(String token, Long id) {
        return webClient.post()
                .uri("/auth/driver-cancellation/requests/" + id + "/approve")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<com.project.ridemesh.dto.DriverCancellationRequestDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<com.project.ridemesh.dto.DriverCancellationRequestDto> rejectCancellationRequest(String token, Long id) {
        return webClient.post()
                .uri("/auth/driver-cancellation/requests/" + id + "/reject")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<com.project.ridemesh.dto.DriverCancellationRequestDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<Map<String, Object>> toggleDriverAvailability(String token) {
        return webClient.post()
                .uri("/auth/driver/toggle-availability")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<com.project.ridemesh.dto.DriverReportDto> submitDriverReport(
            String token, com.project.ridemesh.dto.DriverReportSubmitDto dto) {
        return webClient.post()
                .uri("/auth/driver-reports")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<com.project.ridemesh.dto.DriverReportDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<List<com.project.ridemesh.dto.DriverReportDto>> getDriverReports(String token) {
        return webClient.get()
                .uri("/auth/driver-reports")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<com.project.ridemesh.dto.DriverReportDto>>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<com.project.ridemesh.dto.DriverReportDto> sendReportNotice(
            String token, Long reportId, String noticeMessage) {
        return webClient.post()
                .uri("/auth/driver-reports/" + reportId + "/send-notice")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(Map.of("noticeMessage", noticeMessage))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<com.project.ridemesh.dto.DriverReportDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<com.project.ridemesh.dto.DriverReportDto> suspendDriverFromReport(
            String token, Long reportId) {
        return webClient.post()
                .uri("/auth/driver-reports/" + reportId + "/suspend-driver")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<com.project.ridemesh.dto.DriverReportDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<com.project.ridemesh.dto.UserFeedbackDto> submitUserFeedback(
            String token, com.project.ridemesh.dto.UserFeedbackSubmitDto dto) {
        return webClient.post()
                .uri("/auth/feedback")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<com.project.ridemesh.dto.UserFeedbackDto>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<List<com.project.ridemesh.dto.UserFeedbackDto>> getUserFeedbackList(String token, String type) {
        String uri = (type != null && !type.trim().isEmpty() && !"ALL".equalsIgnoreCase(type))
                ? "/auth/feedback?type=" + type
                : "/auth/feedback";
        return webClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<com.project.ridemesh.dto.UserFeedbackDto>>>() {})
                .map(ApiResponse::getData);
    }

    public Mono<com.project.ridemesh.dto.UserFeedbackDto> replyUserFeedback(
            String token, Long feedbackId, String reply) {
        return webClient.post()
                .uri("/auth/feedback/" + feedbackId + "/reply")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(Map.of("reply", reply))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<com.project.ridemesh.dto.UserFeedbackDto>>() {})
                .map(ApiResponse::getData);
    }
}

