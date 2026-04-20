package com.hirenest.backend.repository;

import com.hirenest.backend.entity.CandidateProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {
    @Query("SELECT c FROM CandidateProfile c WHERE c.user.id = :userId")
    Optional<CandidateProfile> findByUserId(@Param("userId") Long userId);
}

