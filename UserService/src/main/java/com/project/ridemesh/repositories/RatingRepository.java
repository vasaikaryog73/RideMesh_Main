package com.project.ridemesh.repositories;

import com.project.ridemesh.entities.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByRiderId(Long riderId);
    List<Rating> findByDriverId(Long driverId);
    Optional<Rating> findByRideId(Long rideId);
}
