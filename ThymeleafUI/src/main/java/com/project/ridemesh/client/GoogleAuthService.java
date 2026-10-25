package com.project.ridemesh.client;

import com.project.ridemesh.dto.GoogleAuthDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

    @Value("${google.client-id:${GOOGLE_CLIENT_ID:}}")
    private String clientId;

    @Value("${google.client-secret:${GOOGLE_CLIENT_SECRET:}}")
    private String clientSecret;

    @Value("${google.redirect-uri:${GOOGLE_REDIRECT_URI:http://localhost:8085/login/oauth2/code/google}}")
    private String redirectUri;

    private final WebClient webClient;

    public GoogleAuthService() {
        this.webClient = WebClient.builder().build();
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        if (this.clientId == null || this.clientId.isEmpty() || "your-google-client-id".equals(this.clientId)) {
            readDotEnvFallback();
        }
        log.info("GoogleAuthService initialized with clientId: {}", 
                (this.clientId != null && this.clientId.length() > 10) ? this.clientId.substring(0, 10) + "..." : this.clientId);
    }

    private void readDotEnvFallback() {
        java.io.File[] files = new java.io.File[]{
            new java.io.File(".env"),
            new java.io.File("../.env"),
            new java.io.File("../../.env")
        };
        for (java.io.File f : files) {
            if (f.exists()) {
                try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(f))) {
                    String l;
                    while ((l = br.readLine()) != null) {
                        l = l.trim();
                        if (l.startsWith("GOOGLE_CLIENT_ID=")) {
                            this.clientId = l.substring("GOOGLE_CLIENT_ID=".length()).trim();
                        } else if (l.startsWith("GOOGLE_CLIENT_SECRET=")) {
                            this.clientSecret = l.substring("GOOGLE_CLIENT_SECRET=".length()).trim();
                        } else if (l.startsWith("GOOGLE_REDIRECT_URI=")) {
                            this.redirectUri = l.substring("GOOGLE_REDIRECT_URI=".length()).trim();
                        }
                    }
                    if (this.clientId != null && !this.clientId.isEmpty()) break;
                } catch (Exception ignored) {}
            }
        }
    }

    public String buildAuthorizationUrl(String overrideRedirectUri) {
        String effectiveRedirect = (overrideRedirectUri != null && !overrideRedirectUri.isEmpty()) 
                ? overrideRedirectUri : this.redirectUri;
        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(effectiveRedirect, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&scope=" + URLEncoder.encode("openid profile email", StandardCharsets.UTF_8) +
                "&prompt=select_account";
    }

    public GoogleAuthDto fetchGoogleUserInfo(String code, String effectiveRedirectUri) {
        try {
            String redirect = (effectiveRedirectUri != null && !effectiveRedirectUri.isEmpty()) 
                    ? effectiveRedirectUri : this.redirectUri;

            // 1. Exchange authorization code for Google access token
            Map<String, Object> tokenResponse = webClient.post()
                    .uri("https://oauth2.googleapis.com/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("code", code)
                            .with("client_id", clientId)
                            .with("client_secret", clientSecret)
                            .with("redirect_uri", redirect)
                            .with("grant_type", "authorization_code"))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                log.error("Failed to obtain access token from Google token endpoint: {}", tokenResponse);
                return null;
            }

            String accessToken = (String) tokenResponse.get("access_token");

            // 2. Fetch User Profile from Google UserInfo endpoint
            Map<String, Object> userInfo = webClient.get()
                    .uri("https://www.googleapis.com/oauth2/v3/userinfo")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (userInfo == null || !userInfo.containsKey("email")) {
                log.error("Failed to retrieve user profile from Google: {}", userInfo);
                return null;
            }

            String email = (String) userInfo.get("email");
            String name = (String) userInfo.getOrDefault("name", "RideMesh User");
            String picture = (String) userInfo.get("picture");

            return new GoogleAuthDto(email, name, picture);
        } catch (Exception e) {
            log.error("Error during Google OAuth flow: {}", e.getMessage(), e);
            return null;
        }
    }
}
