package com.project.ridemesh.clients;

import com.project.ridemesh.dto.WalletDto;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WalletServiceClient {

    private final RestTemplate restTemplate;
    private static final String WALLET_SERVICE_BASE_URL = "http://WalletService";

    public WalletServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public WalletDto getWalletBalance(Long userId) {
        String url = WALLET_SERVICE_BASE_URL + "/wallet/internal/balance/" + userId;
        try {
            return restTemplate.getForObject(url, WalletDto.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void settleRidePayment(Long rideId, Long riderUserId, Long driverUserId, Double fare, String paymentMethod) {
        String url = WALLET_SERVICE_BASE_URL + "/wallet/internal/settle-ride";
        try {
            java.util.Map<String, Object> req = new java.util.HashMap<>();
            req.put("rideId", rideId);
            req.put("riderUserId", riderUserId);
            req.put("driverUserId", driverUserId);
            req.put("fare", fare);
            req.put("paymentMethod", paymentMethod);
            restTemplate.postForObject(url, req, java.util.Map.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
