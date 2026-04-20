package com.hirenest.backend.repository;

import com.hirenest.backend.entity.Job;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByRecruiterId(Long recruiterId);

    @Query("""
            select j from Job j
            where (:keyword is null or lower(j.title) like lower(concat('%', :keyword, '%'))
                or lower(j.description) like lower(concat('%', :keyword, '%')))
            and (:skill is null or lower(j.requiredSkills) like lower(concat('%', :skill, '%')))
            and (:recruiterId is null or j.recruiter.id = :recruiterId)
            """)
    List<Job> searchJobs(@Param("keyword") String keyword,
                         @Param("skill") String skill,
                         @Param("recruiterId") Long recruiterId);
}

