package com.project.ridemesh.services.implementations;

import com.project.ridemesh.services.DistanceCalculationService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class DistanceCalculationOSRMImpl implements DistanceCalculationService {

    private static final Log log = LogFactory.getLog(DistanceCalculationOSRMImpl.class);
    private static final String OSRM_API_BASE_URL = "https://router.project-osrm.org/route/v1/driving/";

    @Override
    public double calculateDistance(Point src, Point des) {
        try {
            String uri = src.getX() + "," + src.getY() + ";" + des.getX() + "," + des.getY();

            OSRMResponseDto osrmResponse = RestClient
                    .builder()
                    .baseUrl(OSRM_API_BASE_URL)
                    .build()
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body(OSRMResponseDto.class);

            if (osrmResponse != null && osrmResponse.getRoutes() != null && !osrmResponse.getRoutes().isEmpty()) {
                return osrmResponse.getRoutes().get(0).getDistance() / 1000.0;
            }
        } catch (Exception e) {
            log.warn("OSRM route lookup failed, falling back to Haversine straight-line distance. Error: " + e.getMessage());
        }
        
        // Fallback to Haversine straight-line distance
        return calculateHaversineDistance(src, des);
    }

    private double calculateHaversineDistance(Point src, Point des) {
        double lon1 = src.getX();
        double lat1 = src.getY();
        double lon2 = des.getX();
        double lat2 = des.getY();

        double r = 6371.0; // Earth's radius in kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return r * c;
    }
}

class OSRMResponseDto {
    private List<OSRMRoutes> routes;

    public List<OSRMRoutes> getRoutes() {
        return routes;
    }

    public void setRoutes(List<OSRMRoutes> routes) {
        this.routes = routes;
    }
}

class OSRMRoutes {
    private Double distance;

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }
}
