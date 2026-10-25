package com.project.ridemesh.repositories;

import com.project.ridemesh.entities.UserFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {
    List<UserFeedback> findAllByOrderByCreatedAtDesc();
    List<UserFeedback> findByTypeOrderByCreatedAtDesc(String type);
    List<UserFeedback> findByUserIdOrderByCreatedAtDesc(Long userId);
}
