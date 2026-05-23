package com.hirenest.backend.controller;

import com.hirenest.backend.dto.JobDtos;
import com.hirenest.backend.entity.Job;
import com.hirenest.backend.entity.SavedJob;
import com.hirenest.backend.service.JobService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public Job postJob(@RequestBody JobDtos.JobRequest request) {
        return jobService.postJob(request);
    }

    @GetMapping
    public List<Job> allJobs() {
        return jobService.allJobs();
    }

    @GetMapping("/platform")
    public List<Job> platformJobs() {
        return jobService.platformPartnerJobs();
    }

    @GetMapping("/platform/count")
    public Map<String, Object> platformJobCount() {
        Map<String, Object> body = new HashMap<>();
        body.put("count", jobService.platformJobCount());
        body.put("label", Job.PARTNER_NETWORK_LABEL);
        return body;
    }

    @GetMapping("/recruiter/{recruiterId}")
    public List<Job> recruiterJobs(@PathVariable("recruiterId") Long recruiterId) {
        return jobService.recruiterCustomJobs(recruiterId);
    }

    @GetMapping("/recruiter/{recruiterId}/custom")
    public List<Job> recruiterCustomJobs(@PathVariable("recruiterId") Long recruiterId) {
        return jobService.recruiterCustomJobs(recruiterId);
    }

    @GetMapping("/search")
    public List<Job> searchJobs(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "skill", required = false) String skill,
            @RequestParam(value = "recruiterId", required = false) Long recruiterId) {
        return jobService.searchJobs(keyword, skill, recruiterId);
    }

    @PostMapping("/save")
    public SavedJob saveJob(@RequestBody JobDtos.SaveJobRequest request) {
        return jobService.saveJob(request);
    }

    @GetMapping("/saved/{userId}")
    public List<SavedJob> savedJobs(@PathVariable("userId") Long userId) {
        return jobService.savedJobs(userId);
    }
}
