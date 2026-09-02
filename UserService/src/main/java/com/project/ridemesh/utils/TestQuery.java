package com.project.ridemesh.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TestQuery {
    public static void main(String[] args) {
        System.out.println("--- UPDATING DRIVER 8 VEHICLE TYPE TO 'car' ---");
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/user_db", "postgres", "Asus@260422")) {
            String sql = "UPDATE driver SET vehicle_type = 'car' WHERE id = 8";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int rows = pstmt.executeUpdate();
                System.out.println("Rows updated: " + rows);
            }

            // Print updated driver details
            String checkSql = "SELECT * FROM driver WHERE id = 8";
            try (PreparedStatement pstmt = conn.prepareStatement(checkSql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println(String.format("Updated Driver -> ID: %d | Vehicle: %s | District: %s | Available: %b",
                        rs.getLong("id"),
                        rs.getString("vehicle_type"),
                        rs.getString("district"),
                        rs.getBoolean("available")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
