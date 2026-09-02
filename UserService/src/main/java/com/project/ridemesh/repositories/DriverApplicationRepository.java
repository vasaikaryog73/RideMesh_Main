package com.project.ridemesh.repositories;

import com.project.ridemesh.entities.DriverApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DriverApplicationRepository extends JpaRepository<DriverApplication, Long> {
    List<DriverApplication> findByUserId(Long userId);
    List<DriverApplication> findByStatus(String status);
}
