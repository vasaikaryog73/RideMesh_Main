package com.project.ridemesh.clients;

import com.project.ridemesh.dto.DriverDto;
import com.project.ridemesh.dto.PointDto;
import com.project.ridemesh.dto.RiderDto;
import com.project.ridemesh.advices.ApiResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
public class UserServiceClient {

    private final RestTemplate restTemplate;
    private static final String USER_SERVICE_BASE_URL = "http://UserService";

    public UserServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<DriverDto> getNearbyAvailableDrivers(PointDto pickUpLocation, String strategy) {
        String url = USER_SERVICE_BASE_URL + "/internal/drivers/nearby-available?strategy=" + strategy;
        try {
            HttpEntity<PointDto> entity = new HttpEntity<>(pickUpLocation);
            ResponseEntity<ApiResponse<List<DriverDto>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<List<DriverDto>>>() {}
            );
            return response.getBody() != null && response.getBody().getData() != null 
                    ? response.getBody().getData() 
                    : Collections.emptyList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<DriverDto> getMatchingDrivers(List<String> districts, String vehicleType) {
        String districtsParam = String.join(",", districts);
        String url = USER_SERVICE_BASE_URL + "/internal/drivers/matching?vehicleType=" + vehicleType + "&districts=" + districtsParam;
        try {
            ResponseEntity<ApiResponse<List<DriverDto>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResponse<List<DriverDto>>>() {}
            );
            return response.getBody() != null && response.getBody().getData() != null 
                    ? response.getBody().getData() 
                    : Collections.emptyList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public DriverDto getDriverById(Long driverId) {
        String url = USER_SERVICE_BASE_URL + "/internal/drivers/" + driverId;
        try {
            ResponseEntity<ApiResponse<DriverDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResponse<DriverDto>>() {}
            );
            return response.getBody() != null ? response.getBody().getData() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public RiderDto getRiderById(Long riderId) {
        String url = USER_SERVICE_BASE_URL + "/internal/riders/" + riderId;
        try {
            ResponseEntity<ApiResponse<RiderDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResponse<RiderDto>>() {}
            );
            return response.getBody() != null ? response.getBody().getData() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public RiderDto getRiderByUserId(Long userId) {
        String url = USER_SERVICE_BASE_URL + "/internal/riders/user/" + userId;
        try {
            ResponseEntity<ApiResponse<RiderDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResponse<RiderDto>>() {}
            );
            return response.getBody() != null ? response.getBody().getData() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public DriverDto getDriverByUserId(Long userId) {
        String url = USER_SERVICE_BASE_URL + "/internal/drivers/user/" + userId;
        try {
            ResponseEntity<ApiResponse<DriverDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResponse<DriverDto>>() {}
            );
            return response.getBody() != null ? response.getBody().getData() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Double getDriverRatingForRide(Long rideId) {
        String url = USER_SERVICE_BASE_URL + "/internal/ratings/ride/" + rideId;
        try {
            java.util.Map<?, ?> responseMap = restTemplate.getForObject(url, java.util.Map.class);
            if (responseMap != null) {
                Object dataObj = responseMap.get("data");
                java.util.Map<?, ?> targetMap = (dataObj instanceof java.util.Map) ? (java.util.Map<?, ?>) dataObj : responseMap;

                Object ratingVal = targetMap.get("driverRating");
                if (ratingVal == null) {
                    ratingVal = targetMap.get("rating");
                }
                if (ratingVal instanceof Number) {
                    return ((Number) ratingVal).doubleValue();
                } else if (ratingVal != null) {
                    return Double.parseDouble(ratingVal.toString());
                }
            }
        } catch (Exception e) {
            // Not found or not yet rated
        }
        return null;
    }
}

