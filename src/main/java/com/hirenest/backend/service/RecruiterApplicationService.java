package com.hirenest.backend.service;

import com.hirenest.backend.dto.ApplicationDtos.JobApplicationResponse;
import com.hirenest.backend.entity.Job;
import com.hirenest.backend.entity.JobApplication;
import com.hirenest.backend.exception.NotFoundException;
import com.hirenest.backend.mapper.ApplicationMapper;
import com.hirenest.backend.repository.JobApplicationRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RecruiterApplicationService {
    private static final Set<String> ALLOWED_STATUS = Set.of("APPLIED", "SHORTLISTED", "REJECTED");
    private final JobService jobService;
    private final JobApplicationRepository jobApplicationRepository;

    public RecruiterApplicationService(JobService jobService, JobApplicationRepository jobApplicationRepository) {
        this.jobService = jobService;
        this.jobApplicationRepository = jobApplicationRepository;
    }

    public List<JobApplication> recruiterApplicationsEntities(Long recruiterId) {
        List<Job> jobs = jobService.recruiterJobs(recruiterId);
        if (jobs.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> jobIds = jobs.stream().map(Job::getId).toList();
        return jobApplicationRepository.findByJobIdIn(jobIds);
    }

    public List<JobApplicationResponse> recruiterApplications(Long recruiterId) {
        return recruiterApplicationsEntities(recruiterId).stream()
                .map(ApplicationMapper::toResponse)
                .collect(Collectors.toList());
    }

    public JobApplicationResponse updateStatus(Long applicationId, String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUS.contains(normalized)) {
            throw new NotFoundException("Unsupported status. Use APPLIED, SHORTLISTED, or REJECTED");
        }
        JobApplication app = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));
        LocalDateTime now = LocalDateTime.now();
        if ("SHORTLISTED".equals(normalized)) {
            app.setShortlistedAt(now);
        }
        if ("REJECTED".equals(normalized)) {
            app.setRejectedAt(now);
        }
        app.setStatus(normalized);
        JobApplication saved = jobApplicationRepository.save(app);
        return ApplicationMapper.toResponse(saved);
    }

    public long countByStatus(Long recruiterId, String status) {
        String normalized = status.toUpperCase(Locale.ROOT);
        return recruiterApplicationsEntities(recruiterId).stream()
                .filter(a -> normalized.equals(a.getStatus()))
                .count();
    }
}

