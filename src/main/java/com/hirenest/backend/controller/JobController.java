package com.hirenest.backend.controller;

import com.hirenest.backend.dto.JobDtos.JobRequest;
import com.hirenest.backend.dto.JobDtos.SaveJobRequest;
import com.hirenest.backend.entity.Job;
import com.hirenest.backend.entity.SavedJob;
import com.hirenest.backend.service.JobService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin
public class JobController {
    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public Job postJob(@RequestBody JobRequest request) {
        return jobService.postJob(request);
    }

    @GetMapping
    public List<Job> allJobs() {
        return jobService.allJobs();
    }

    @GetMapping("/recruiter/{recruiterId}")
    public List<Job> recruiterJobs(@PathVariable Long recruiterId) {
        return jobService.recruiterJobs(recruiterId);
    }

    @GetMapping("/search")
    public List<Job> searchJobs(@RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String skill,
                                @RequestParam(required = false) Long recruiterId) {
        return jobService.searchJobs(keyword, skill, recruiterId);
    }

    @PostMapping("/save")
    public SavedJob saveJob(@RequestBody SaveJobRequest request) {
        return jobService.saveJob(request);
    }

    @GetMapping("/saved/{userId}")
    public List<SavedJob> savedJobs(@PathVariable Long userId) {
        return jobService.savedJobs(userId);
    }
}

