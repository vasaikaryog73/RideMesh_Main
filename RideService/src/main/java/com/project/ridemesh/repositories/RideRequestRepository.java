package com.project.ridemesh.repositories;

import com.project.ridemesh.entities.RideRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.ridemesh.entities.enums.RideRequestStatus;
import java.util.List;

@Repository
public interface RideRequestRepository extends JpaRepository<RideRequest, Long> {
    List<RideRequest> findByStatus(RideRequestStatus status);
}
