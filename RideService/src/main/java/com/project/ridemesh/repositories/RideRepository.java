package com.project.ridemesh.repositories;

import com.project.ridemesh.entities.Ride;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.ridemesh.entities.enums.RideStatus;
import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {
    Page<Ride> findByRiderId(Long riderId, Pageable pageRequest);
    Page<Ride> findByDriverId(Long driverId, Pageable pageRequest);
    List<Ride> findByRiderIdAndStatus(Long riderId, RideStatus status);
    List<Ride> findByDriverIdAndStatus(Long driverId, RideStatus status);
    List<Ride> findByRiderIdAndStatusIn(Long riderId, List<RideStatus> statuses);
    List<Ride> findByDriverIdAndStatusIn(Long driverId, List<RideStatus> statuses);
}
