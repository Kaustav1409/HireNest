package com.hirenest.backend.repository;

import com.hirenest.backend.entity.QuizAttempt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findTop5ByUserIdOrderByAttemptedAtDesc(Long userId);
    List<QuizAttempt> findByUserId(Long userId);
    List<QuizAttempt> findByUserIdAndStatusIgnoreCaseOrderByAttemptedAtDesc(Long userId, String status);

    Optional<QuizAttempt> findFirstByUserIdOrderByAttemptedAtDesc(Long userId);
    Optional<QuizAttempt> findTopByUserIdAndSkillIgnoreCaseOrderByAttemptedAtDesc(Long userId, String skill);
    Optional<QuizAttempt> findByIdAndUserId(Long id, Long userId);
}

