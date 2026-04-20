package com.hirenest.backend.service;

import com.hirenest.backend.dto.JobDtos.JobRequest;
import com.hirenest.backend.dto.JobDtos.SaveJobRequest;
import com.hirenest.backend.entity.Job;
import com.hirenest.backend.entity.SavedJob;
import com.hirenest.backend.entity.User;
import com.hirenest.backend.exception.BadRequestException;
import com.hirenest.backend.exception.NotFoundException;
import com.hirenest.backend.repository.JobRepository;
import com.hirenest.backend.repository.SavedJobRepository;
import com.hirenest.backend.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class JobService {
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final SavedJobRepository savedJobRepository;

    public JobService(JobRepository jobRepository, UserRepository userRepository, SavedJobRepository savedJobRepository) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.savedJobRepository = savedJobRepository;
    }

    public Job postJob(JobRequest request) {
        if (request == null || request.title == null || request.title.isBlank()) {
            throw new BadRequestException("Job title is required");
        }
        if (request.requiredSkills == null || request.requiredSkills.isBlank()) {
            throw new BadRequestException("requiredSkills is required");
        }
        User recruiter = userRepository.findById(request.recruiterId)
                .orElseThrow(() -> new NotFoundException("Recruiter not found"));
        Job job = new Job();
        job.setRecruiter(recruiter);
        job.setTitle(request.title);
        job.setCompanyName(request.companyName);
        job.setDescription(request.description);
        job.setRequiredSkills(request.requiredSkills);
        job.setLocation(request.location);
        job.setRemote(request.remote);
        job.setSalaryMin(request.salaryMin);
        job.setSalaryMax(request.salaryMax);
        return jobRepository.save(job);
    }

    public List<Job> allJobs() {
        return jobRepository.findAll();
    }

    public List<Job> recruiterJobs(Long recruiterId) {
        return jobRepository.findByRecruiterId(recruiterId);
    }

    public List<Job> searchJobs(String keyword, String skill, Long recruiterId) {
        return jobRepository.searchJobs(keyword, skill, recruiterId);
    }

    public SavedJob saveJob(SaveJobRequest request) {
        SavedJob existing = savedJobRepository.findByUserIdAndJobId(request.userId, request.jobId).orElse(null);
        if (existing != null) {
            return existing;
        }
        User user = userRepository.findById(request.userId).orElseThrow(() -> new NotFoundException("User not found"));
        Job job = jobRepository.findById(request.jobId).orElseThrow(() -> new NotFoundException("Job not found"));
        SavedJob savedJob = new SavedJob();
        savedJob.setUser(user);
        savedJob.setJob(job);
        return savedJobRepository.save(savedJob);
    }

    public List<SavedJob> savedJobs(Long userId) {
        return savedJobRepository.findByUserId(userId);
    }
}

