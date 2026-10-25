package com.project.ridemesh.repositories;

import com.project.ridemesh.entities.DriverReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverReportRepository extends JpaRepository<DriverReport, Long> {

    List<DriverReport> findAllByOrderByCreatedAtDesc();

    List<DriverReport> findByDriverId(Long driverId);

    long countByDriverId(Long driverId);
}
