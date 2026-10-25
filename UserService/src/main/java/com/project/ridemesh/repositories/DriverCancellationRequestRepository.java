package com.project.ridemesh.repositories;

import com.project.ridemesh.entities.DriverCancellationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverCancellationRequestRepository extends JpaRepository<DriverCancellationRequest, Long> {
    Optional<DriverCancellationRequest> findTopByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
    Optional<DriverCancellationRequest> findTopByUserIdOrderByCreatedAtDesc(Long userId);
    List<DriverCancellationRequest> findAllByOrderByCreatedAtDesc();
    List<DriverCancellationRequest> findByStatusOrderByCreatedAtDesc(String status);
}
