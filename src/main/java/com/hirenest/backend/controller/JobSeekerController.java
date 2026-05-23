package com.hirenest.backend.controller;

import com.hirenest.backend.dto.ApplicationDtos;
import com.hirenest.backend.entity.SavedJob;
import com.hirenest.backend.service.JobSeekerActionService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/job-seeker")
@CrossOrigin
public class JobSeekerController {

    private final JobSeekerActionService jobSeekerActionService;

    public JobSeekerController(JobSeekerActionService jobSeekerActionService) {
        this.jobSeekerActionService = jobSeekerActionService;
    }

    @PostMapping("/{userId}/save/{jobId}")
    public SavedJob saveJob(@PathVariable("userId") Long userId, @PathVariable("jobId") Long jobId) {
        return jobSeekerActionService.saveJob(userId, jobId);
    }

    @PostMapping("/{userId}/apply/{jobId}")
    public ApplicationDtos.JobApplicationResponse applyJob(@PathVariable("userId") Long userId, @PathVariable("jobId") Long jobId) {
        return jobSeekerActionService.applyJob(userId, jobId);
    }

    @PostMapping("/{userId}/apply/{jobId}/preview")
    public ApplicationDtos.ApplicationStrengthPreview previewApply(
            @PathVariable("userId") Long userId,
            @PathVariable("jobId") Long jobId,
            @RequestBody(required = false) ApplicationDtos.EnhancedApplyRequest request) {
        return jobSeekerActionService.previewApplicationStrength(userId, jobId, request);
    }

    @PostMapping(value = "/{userId}/apply/{jobId}/enhanced", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApplicationDtos.JobApplicationResponse applyJobEnhanced(
            @PathVariable("userId") Long userId,
            @PathVariable("jobId") Long jobId,
            @RequestPart(value = "payload", required = false) String payloadJson,
            @RequestParam(required = false) String githubLink,
            @RequestParam(required = false) String portfolioLink,
            @RequestParam(required = false) String driveLink,
            @RequestParam(required = false) String projectDescription,
            @RequestParam(required = false) String interestedRoles,
            @RequestParam(required = false) String strongestSkills,
            @RequestParam(required = false) String contributionDescription,
            @RequestParam(required = false) String availabilityTypes,
            @RequestPart(value = "resume", required = false) MultipartFile resume,
            @RequestPart(value = "projectFile", required = false) MultipartFile projectFile) {
        ApplicationDtos.EnhancedApplyRequest request = payloadJson != null && !payloadJson.isBlank()
                ? jobSeekerActionService.parseApplyRequest(payloadJson)
                : buildRequestFromParams(
                        githubLink,
                        portfolioLink,
                        driveLink,
                        projectDescription,
                        interestedRoles,
                        strongestSkills,
                        contributionDescription,
                        availabilityTypes);
        return jobSeekerActionService.applyJobEnhanced(userId, jobId, request, resume, projectFile);
    }

    @GetMapping("/{userId}/saved-jobs")
    public List<SavedJob> savedJobs(@PathVariable("userId") Long userId) {
        return jobSeekerActionService.savedJobs(userId);
    }

    @GetMapping("/{userId}/applications")
    public List<ApplicationDtos.JobApplicationResponse> applications(@PathVariable("userId") Long userId) {
        return jobSeekerActionService.applications(userId);
    }

    private static ApplicationDtos.EnhancedApplyRequest buildRequestFromParams(
            String githubLink,
            String portfolioLink,
            String driveLink,
            String projectDescription,
            String interestedRoles,
            String strongestSkills,
            String contributionDescription,
            String availabilityTypes) {
        ApplicationDtos.EnhancedApplyRequest req = new ApplicationDtos.EnhancedApplyRequest();
        req.githubLink = githubLink;
        req.portfolioLink = portfolioLink;
        req.driveLink = driveLink;
        req.projectDescription = projectDescription;
        req.interestedRoles = splitCsv(interestedRoles);
        req.strongestSkills = splitCsv(strongestSkills);
        req.contributionDescription = contributionDescription;
        req.availabilityTypes = splitCsv(availabilityTypes);
        return req;
    }

    private static List<String> splitCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return List.of(raw.split("\\s*,\\s*"));
    }
}
