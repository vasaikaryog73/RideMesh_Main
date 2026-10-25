package com.project.ridemesh.booking.repositories;

import com.project.ridemesh.booking.entities.Booking;
import com.project.ridemesh.booking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Booking> findByStatusAndWorkerIdIsNullOrderByCreatedAtDesc(BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.status = :status AND (b.workerId IS NULL OR b.workerId = :workerId) ORDER BY b.createdAt DESC")
    List<Booking> findPendingBookingsForWorker(@Param("status") BookingStatus status, @Param("workerId") Long workerId);

    List<Booking> findByWorkerIdOrderByCreatedAtDesc(Long workerId);
}
