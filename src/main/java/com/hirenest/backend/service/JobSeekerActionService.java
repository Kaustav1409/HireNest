package com.hirenest.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hirenest.backend.dto.ApplicationDtos;
import com.hirenest.backend.entity.Job;
import com.hirenest.backend.entity.JobApplication;
import com.hirenest.backend.entity.SavedJob;
import com.hirenest.backend.entity.User;
import com.hirenest.backend.exception.BadRequestException;
import com.hirenest.backend.exception.NotFoundException;
import com.hirenest.backend.mapper.ApplicationMapper;
import com.hirenest.backend.repository.JobApplicationRepository;
import com.hirenest.backend.repository.JobRepository;
import com.hirenest.backend.repository.SavedJobRepository;
import com.hirenest.backend.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class JobSeekerActionService {

    private static final long MAX_UPLOAD_BYTES = 10L * 1024L * 1024L;

    private final SavedJobRepository savedJobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ApplicationStrengthService applicationStrengthService;
    private final ObjectMapper objectMapper;

    public JobSeekerActionService(
            SavedJobRepository savedJobRepository,
            JobApplicationRepository jobApplicationRepository,
            UserRepository userRepository,
            JobRepository jobRepository,
            ApplicationStrengthService applicationStrengthService,
            ObjectMapper objectMapper) {
        this.savedJobRepository = savedJobRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.applicationStrengthService = applicationStrengthService;
        this.objectMapper = objectMapper;
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

    public ApplicationDtos.JobApplicationResponse applyJob(Long userId, Long jobId) {
        return applyJobEnhanced(userId, jobId, null, null, null);
    }

    public ApplicationDtos.ApplicationStrengthPreview previewApplicationStrength(
            Long userId,
            Long jobId,
            ApplicationDtos.EnhancedApplyRequest request) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new NotFoundException("Job not found"));
        userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        return applicationStrengthService.analyze(userId, job, request);
    }

    public ApplicationDtos.JobApplicationResponse applyJobEnhanced(
            Long userId,
            Long jobId,
            ApplicationDtos.EnhancedApplyRequest request,
            MultipartFile resumeFile,
            MultipartFile projectFile) {
        JobApplication existing = jobApplicationRepository.findByUserIdAndJobId(userId, jobId).orElse(null);
        if (existing != null) {
            return ApplicationMapper.toResponse(existing);
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new NotFoundException("Job not found"));

        ApplicationDtos.EnhancedApplyRequest req = request == null ? new ApplicationDtos.EnhancedApplyRequest() : request;
        ApplicationDtos.ApplicationStrengthPreview strength =
                applicationStrengthService.analyze(userId, job, req);

        JobApplication app = new JobApplication();
        app.setUser(user);
        app.setJob(job);
        app.setStatus("APPLIED");
        app.setGithubLink(trim(req.githubLink));
        app.setPortfolioLink(trim(req.portfolioLink));
        app.setDriveLink(trim(req.driveLink));
        app.setProjectDescription(trim(req.projectDescription));
        app.setInterestedRoles(joinList(req.interestedRoles));
        app.setStrongestSkills(joinList(req.strongestSkills));
        app.setContributionDescription(trim(req.contributionDescription));
        app.setAvailabilityTypes(joinList(req.availabilityTypes));
        app.setApplicationStrengthPercent(strength.applicationStrengthPercent);
        app.setHiringConfidencePercent(strength.hiringConfidencePercent);
        app.setStrengthInsights(strength.strengthInsights);

        if (resumeFile != null && !resumeFile.isEmpty()) {
            app.setResumeSnapshotPath(storeUpload(userId, "apply-resume", resumeFile));
        }
        if (projectFile != null && !projectFile.isEmpty()) {
            app.setProjectFilePath(storeUpload(userId, "apply-project", projectFile));
        }

        JobApplication saved = jobApplicationRepository.save(app);
        return ApplicationMapper.toResponse(saved);
    }

    public ApplicationDtos.EnhancedApplyRequest parseApplyRequest(String json) {
        if (json == null || json.isBlank()) {
            return new ApplicationDtos.EnhancedApplyRequest();
        }
        try {
            return objectMapper.readValue(json, ApplicationDtos.EnhancedApplyRequest.class);
        } catch (IOException ex) {
            throw new BadRequestException("Invalid application payload JSON");
        }
    }

    public List<SavedJob> savedJobs(Long userId) {
        return savedJobRepository.findByUserId(userId);
    }

    public List<ApplicationDtos.JobApplicationResponse> applications(Long userId) {
        return jobApplicationRepository.findByUserId(userId).stream()
                .map(ApplicationMapper::toResponse)
                .collect(Collectors.toList());
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static String joinList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        return items.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim())
                .collect(Collectors.joining(", "));
    }

    private String storeUpload(Long userId, String prefix, MultipartFile file) {
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new BadRequestException("File exceeds 10MB limit");
        }
        String original = file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename();
        String safe = original.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase(Locale.ROOT);
        try {
            Path dir = Path.of("uploads", "applications", String.valueOf(userId));
            Files.createDirectories(dir);
            String filename = prefix + "_" + System.currentTimeMillis() + "_" + safe;
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString().replace('\\', '/');
        } catch (IOException ex) {
            throw new BadRequestException("Could not store uploaded file");
        }
    }
}
