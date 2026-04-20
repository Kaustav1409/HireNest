package com.hirenest.backend.repository;

import com.hirenest.backend.entity.RecruiterProfile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecruiterProfileRepository extends JpaRepository<RecruiterProfile, Long> {
    /**
     * All rows for a user, stable order. Prefer the first row for upsert so duplicate DB rows do not break
     * {@code Optional} single-result queries (which throw {@code IncorrectResultSizeDataAccessException}).
     */
    @Query("SELECT r FROM RecruiterProfile r WHERE r.user.id = :userId ORDER BY r.id ASC")
    List<RecruiterProfile> findAllByUserIdOrderByIdAsc(@Param("userId") Long userId);
}

