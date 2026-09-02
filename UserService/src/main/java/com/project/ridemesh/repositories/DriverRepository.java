package com.project.ridemesh.repositories;

import com.project.ridemesh.entities.Driver;
import com.project.ridemesh.entities.User;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    @Query(value = "SELECT d.*, ST_Distance(d.current_location, :pickUpLocation) AS distance " +
            "FROM driver d " +
            "WHERE d.available = true AND ST_DWithin(d.current_location, :pickUpLocation, 100000) " +
            "ORDER BY distance " +
            "LIMIT 50", nativeQuery = true)
    List<Driver> findTenNearestDrivers(Point pickUpLocation);

    @Query(value = "SELECT d.* " +
            "FROM driver d " +
            "WHERE d.available = true AND ST_DWithin(d.current_location, :pickUpLocation, 100000) " +
            "ORDER BY d.rating DESC " +
            "LIMIT 50" , nativeQuery = true)
    List<Driver> findTenNearbyTopRatedDrivers(Point pickUpLocation);

    Optional<Driver> findByUser(User user);

    @Query("SELECT d FROM Driver d WHERE d.available = true AND LOWER(d.vehicleType) = LOWER(:vehicleType) AND LOWER(d.district) IN :districts")
    List<Driver> findMatchingDrivers(String vehicleType, List<String> districts);
}
