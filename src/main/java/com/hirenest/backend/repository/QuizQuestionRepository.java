package com.hirenest.backend.repository;

import com.hirenest.backend.entity.QuizQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findBySkillIgnoreCase(String skill);

    List<QuizQuestion> findBySkillIgnoreCaseAndDifficultyIgnoreCase(String skill, String difficulty);

    long countBySkillIgnoreCase(String skill);

    long countBySkillIgnoreCaseAndDifficultyIgnoreCase(String skill, String difficulty);

    @Modifying
    @Transactional
    void deleteBySkillIgnoreCase(String skill);
}

