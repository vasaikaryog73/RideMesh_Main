package com.project.ridemesh.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LocationUtil {

    public static List<String> getAdjacentDistricts(String district) {
        if (district == null) return Collections.emptyList();
        String d = district.toLowerCase().trim();
        switch (d) {
            case "mumbai":
                return Arrays.asList("Thane", "Palghar");
            case "thane":
                return Arrays.asList("Mumbai", "Palghar", "Pune", "Nashik");
            case "palghar":
                return Arrays.asList("Mumbai", "Thane", "Nashik");
            case "pune":
                return Arrays.asList("Thane", "Satara", "Ahmednagar", "Solapur");
            case "nashik":
                return Arrays.asList("Thane", "Palghar", "Ahmednagar", "Dhule");
            case "bengaluru urban":
                return Arrays.asList("Bengaluru Rural", "Ramanagara", "Chikkaballapura");
            case "bengaluru rural":
                return Arrays.asList("Bengaluru Urban", "Ramanagara", "Tumakuru", "Kolar");
            case "kolar":
                return Arrays.asList("Bengaluru Rural", "Chikkaballapura");
            case "tumakuru":
                return Arrays.asList("Bengaluru Rural", "Chitradurga", "Hassan");
            case "new delhi":
                return Arrays.asList("South Delhi", "Central Delhi", "West Delhi");
            case "south delhi":
                return Arrays.asList("New Delhi", "South West Delhi", "East Delhi");
            case "central delhi":
                return Arrays.asList("New Delhi", "North Delhi", "West Delhi");
            default:
                return Collections.emptyList();
        }
    }

    public static boolean isDistrictMatch(String driverDistrict, String reqDistrict) {
        if (driverDistrict == null || driverDistrict.isBlank() || reqDistrict == null || reqDistrict.isBlank()) {
            return true;
        }
        String d1 = cleanDistrictName(driverDistrict);
        String d2 = cleanDistrictName(reqDistrict);

        if (d1.equals(d2)) return true;
        if (d1.contains(d2) || d2.contains(d1)) return true;

        if ((d1.contains("mumbai") && d2.contains("mumbai")) ||
            (d1.contains("bengaluru") && d2.contains("bengaluru")) ||
            (d1.contains("bangalore") && (d2.contains("bangalore") || d2.contains("bengaluru"))) ||
            (d1.contains("delhi") && d2.contains("delhi"))) {
            return true;
        }

        return false;
    }

    public static String cleanDistrictName(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replaceAll("\\b(district|dist|city|suburban|rural|urban)\\b", "")
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }

    public static boolean isVehicleTypeMatch(String driverVehicleType, String requestedVehicleType) {
        if (requestedVehicleType == null || requestedVehicleType.isBlank()) {
            return true;
        }
        if (driverVehicleType == null || driverVehicleType.isBlank()) {
            return false;
        }
        String d = driverVehicleType.trim().toLowerCase();
        String r = requestedVehicleType.trim().toLowerCase();

        if (d.equals(r)) {
            return true;
        }

        // Scooter and scooty alias
        if ((d.equals("scooter") || d.equals("scooty")) && (r.equals("scooter") || r.equals("scooty"))) {
            return true;
        }

        // Auto / Rickshaw alias
        if ((d.contains("rickshaw") || d.contains("auto")) && (r.contains("rickshaw") || r.contains("auto"))) {
            return true;
        }

        return false;
    }
}
