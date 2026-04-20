package com.hirenest.backend.repository;

import com.hirenest.backend.entity.JobApplication;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findByUserId(Long userId);
    Optional<JobApplication> findByUserIdAndJobId(Long userId, Long jobId);
    List<JobApplication> findByJobIdIn(List<Long> jobIds);

    long countByUserId(Long userId);
}

