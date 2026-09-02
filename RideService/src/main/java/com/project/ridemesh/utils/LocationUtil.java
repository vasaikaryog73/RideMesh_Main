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
}
