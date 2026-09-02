package com.project.ridemesh.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UpdateDriverLocations {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/user_db?useSSL=false";
        String user = "postgres";
        String password = "Asus@260422";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connected to the PostgreSQL database successfully.");

            // 1. Update driver table locations
            String selectSql = "SELECT id, district FROM driver";
            try (PreparedStatement pstmt = conn.prepareStatement(selectSql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String district = rs.getString("district");
                    double[] coords = getDistrictCenter(district);
                    double lon = coords[0];
                    double lat = coords[1];
                    
                    String updateSql = "UPDATE driver SET current_location = ST_SetSRID(ST_MakePoint(?, ?), 4326) WHERE id = ?";
                    try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                        updatePstmt.setDouble(1, lon);
                        updatePstmt.setDouble(2, lat);
                        updatePstmt.setLong(3, id);
                        int rows = updatePstmt.executeUpdate();
                        System.out.printf("Updated driver ID=%d (%s) location to lon=%.4f, lat=%.4f (Rows affected: %d)%n",
                                id, district, lon, lat, rows);
                    }
                }
            }

            // 2. Update driver_application table locations
            String selectAppSql = "SELECT id, district FROM driver_application WHERE longitude IS NULL OR latitude IS NULL";
            try (PreparedStatement pstmt = conn.prepareStatement(selectAppSql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String district = rs.getString("district");
                    double[] coords = getDistrictCenter(district);
                    double lon = coords[0];
                    double lat = coords[1];
                    
                    String updateAppSql = "UPDATE driver_application SET longitude = ?, latitude = ? WHERE id = ?";
                    try (PreparedStatement updatePstmt = conn.prepareStatement(updateAppSql)) {
                        updatePstmt.setDouble(1, lon);
                        updatePstmt.setDouble(2, lat);
                        updatePstmt.setLong(3, id);
                        int rows = updatePstmt.executeUpdate();
                        System.out.printf("Updated driver_application ID=%d (%s) location to lon=%.4f, lat=%.4f (Rows affected: %d)%n",
                                id, district, lon, lat, rows);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double[] getDistrictCenter(String district) {
        if (district == null) return new double[]{72.8600, 19.0400};
        String d = district.toLowerCase().trim();
        switch (d) {
            case "mumbai suburban":
                return new double[]{72.8368, 19.1076}; // Vile Parle
            case "mumbai":
                return new double[]{72.8258, 18.9750};
            case "thane":
                return new double[]{72.9781, 19.2183};
            case "palghar":
                return new double[]{72.7656, 19.6967};
            case "pune":
                return new double[]{73.8567, 18.5204};
            case "nashik":
                return new double[]{73.7898, 19.9975};
            case "bengaluru urban":
                return new double[]{77.5946, 12.9716};
            case "bengaluru rural":
                return new double[]{77.5750, 13.2847};
            case "kolar":
                return new double[]{78.1292, 13.1368};
            case "tumakuru":
                return new double[]{77.1006, 13.3409};
            case "new delhi":
            case "south delhi":
            case "central delhi":
            case "south west delhi":
            case "west delhi":
            case "east delhi":
                return new double[]{77.2090, 28.6139};
            default:
                return new double[]{72.8600, 19.0400};
        }
    }
}
