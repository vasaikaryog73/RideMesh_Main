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
}
