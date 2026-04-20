package com.hirenest.backend.controller;

import com.hirenest.backend.dto.ApplicationDtos.JobApplicationResponse;
import com.hirenest.backend.entity.SavedJob;
import com.hirenest.backend.service.JobSeekerActionService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/job-seeker")
@CrossOrigin
public class JobSeekerController {
    private final JobSeekerActionService jobSeekerActionService;

    public JobSeekerController(JobSeekerActionService jobSeekerActionService) {
        this.jobSeekerActionService = jobSeekerActionService;
    }

    @PostMapping("/{userId}/save/{jobId}")
    public SavedJob saveJob(@PathVariable Long userId, @PathVariable Long jobId) {
        return jobSeekerActionService.saveJob(userId, jobId);
    }

    @PostMapping("/{userId}/apply/{jobId}")
    public JobApplicationResponse applyJob(@PathVariable Long userId, @PathVariable Long jobId) {
        return jobSeekerActionService.applyJob(userId, jobId);
    }

    @GetMapping("/{userId}/saved-jobs")
    public List<SavedJob> savedJobs(@PathVariable Long userId) {
        return jobSeekerActionService.savedJobs(userId);
    }

    @GetMapping("/{userId}/applications")
    public List<JobApplicationResponse> applications(@PathVariable Long userId) {
        return jobSeekerActionService.applications(userId);
    }
}

