package com.hirenest.backend.service;

import com.hirenest.backend.dto.ApplicationDtos.JobApplicationResponse;
import com.hirenest.backend.entity.Job;
import com.hirenest.backend.entity.JobApplication;
import com.hirenest.backend.entity.SavedJob;
import com.hirenest.backend.entity.User;
import com.hirenest.backend.exception.NotFoundException;
import com.hirenest.backend.mapper.ApplicationMapper;
import com.hirenest.backend.repository.JobApplicationRepository;
import com.hirenest.backend.repository.JobRepository;
import com.hirenest.backend.repository.SavedJobRepository;
import com.hirenest.backend.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class JobSeekerActionService {
    private final SavedJobRepository savedJobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;

    public JobSeekerActionService(SavedJobRepository savedJobRepository,
                                  JobApplicationRepository jobApplicationRepository,
                                  UserRepository userRepository,
                                  JobRepository jobRepository) {
        this.savedJobRepository = savedJobRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
    }

    public SavedJob saveJob(Long userId, Long jobId) {
        SavedJob existing = savedJobRepository.findByUserIdAndJobId(userId, jobId).orElse(null);
        if (existing != null) {
            return existing;
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new NotFoundException("Job not found"));
        SavedJob savedJob = new SavedJob();
        savedJob.setUser(user);
        savedJob.setJob(job);
        return savedJobRepository.save(savedJob);
    }

    public JobApplicationResponse applyJob(Long userId, Long jobId) {
        JobApplication existing = jobApplicationRepository.findByUserIdAndJobId(userId, jobId).orElse(null);
        if (existing != null) {
            return ApplicationMapper.toResponse(existing);
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new NotFoundException("Job not found"));
        JobApplication app = new JobApplication();
        app.setUser(user);
        app.setJob(job);
        app.setStatus("APPLIED");
        JobApplication saved = jobApplicationRepository.save(app);
        return ApplicationMapper.toResponse(saved);
    }

    public List<SavedJob> savedJobs(Long userId) {
        return savedJobRepository.findByUserId(userId);
    }

    public List<JobApplicationResponse> applications(Long userId) {
        return jobApplicationRepository.findByUserId(userId).stream()
                .map(ApplicationMapper::toResponse)
                .collect(Collectors.toList());
    }
}

