package com.project.ridemesh.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class DeleteAllApplications {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/user_db?useSSL=false";
        String user = "postgres";
        String password = "Asus@260422";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connected to the PostgreSQL database successfully.");

            String deleteSql = "DELETE FROM driver_application";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                int affectedRows = pstmt.executeUpdate();
                System.out.println("Successfully deleted " + affectedRows + " row(s) from driver_application table.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
