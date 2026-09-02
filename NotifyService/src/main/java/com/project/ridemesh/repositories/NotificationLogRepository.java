package com.project.ridemesh.repositories;

import com.project.ridemesh.entities.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    Optional<NotificationLog> findByEventIdAndRecipientEmail(String eventId, String recipientEmail);
}
