package com.hirenest.backend.controller;

import com.hirenest.backend.service.DashboardService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/job-seeker/{userId}")
    public Map<String, Object> jobSeekerDashboard(@PathVariable Long userId) {
        return dashboardService.jobSeekerDashboard(userId);
    }

    @GetMapping("/recruiter/{recruiterId}")
    public Map<String, Object> recruiterDashboard(@PathVariable Long recruiterId) {
        return dashboardService.recruiterDashboard(recruiterId);
    }

    @GetMapping("/job-seeker/{userId}/insights")
    public Map<String, Object> jobSeekerInsights(@PathVariable Long userId) {
        return dashboardService.jobSeekerInsights(userId);
    }

    @GetMapping("/recruiter/{recruiterId}/insights")
    public Map<String, Object> recruiterInsights(@PathVariable Long recruiterId) {
        return dashboardService.recruiterInsights(recruiterId);
    }
}

