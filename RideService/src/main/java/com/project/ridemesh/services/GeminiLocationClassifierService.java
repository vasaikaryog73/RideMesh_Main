package com.project.ridemesh.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiLocationClassifierService {

    private static final Logger log = LoggerFactory.getLogger(GeminiLocationClassifierService.class);

    @Value("${gemini.api.key:${GEMINI_API_KEY:}}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=%s";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiLocationClassifierService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Classifies pickup coordinates (latitude, longitude) into [State, District] in India using Google Gemini API.
     * Returns new String[]{state, district} on success, or null on failure/timeout.
     */
    public String[] classifyLocation(double lat, double lon) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return null;
        }

        try {
            String url = String.format(GEMINI_API_URL_TEMPLATE, geminiApiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String prompt = String.format(
                    "Identify the State and District in India for pickup coordinates latitude %f, longitude %f. " +
                    "Respond ONLY with a valid JSON object in this exact format: {\"state\": \"...\", \"district\": \"...\"} " +
                    "with no markdown formatting, no code fences, and no extra text.",
                    lat, lon
            );

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", Collections.singletonList(textPart));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", Collections.singletonList(content));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode parts = candidates.get(0).path("content").path("parts");
                    if (parts.isArray() && !parts.isEmpty()) {
                        String rawText = parts.get(0).path("text").asText();
                        log.info("Gemini location classifier raw response: {}", rawText);

                        String cleanedJson = rawText.trim();
                        if (cleanedJson.startsWith("```json")) {
                            cleanedJson = cleanedJson.substring(7);
                        } else if (cleanedJson.startsWith("```")) {
                            cleanedJson = cleanedJson.substring(3);
                        }
                        if (cleanedJson.endsWith("```")) {
                            cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
                        }
                        cleanedJson = cleanedJson.trim();

                        JsonNode locationNode = objectMapper.readTree(cleanedJson);
                        String state = locationNode.path("state").asText(null);
                        String district = locationNode.path("district").asText(null);

                        if (state != null && !state.isBlank() && district != null && !district.isBlank()) {
                            log.info("Gemini successfully classified pickup location ({}, {}) -> State: '{}', District: '{}'",
                                    lat, lon, state.trim(), district.trim());
                            return new String[]{state.trim(), district.trim()};
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Gemini location classification request failed: {}. Falling back to default geocoders.", e.getMessage());
        }
        return null;
    }
}
