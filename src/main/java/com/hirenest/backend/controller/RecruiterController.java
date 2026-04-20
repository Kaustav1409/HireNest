package com.hirenest.backend.controller;

import com.hirenest.backend.dto.ApplicationDtos.JobApplicationResponse;
import com.hirenest.backend.dto.RecruiterDtos.RankedCandidateDto;
import com.hirenest.backend.service.RecruiterApplicationService;
import com.hirenest.backend.service.RecruiterCandidateService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recruiter")
@CrossOrigin
public class RecruiterController {
    private final RecruiterCandidateService recruiterCandidateService;
    private final RecruiterApplicationService recruiterApplicationService;

    public RecruiterController(RecruiterCandidateService recruiterCandidateService,
                               RecruiterApplicationService recruiterApplicationService) {
        this.recruiterCandidateService = recruiterCandidateService;
        this.recruiterApplicationService = recruiterApplicationService;
    }

    @GetMapping("/candidates/{recruiterId}")
    public Map<Long, List<RankedCandidateDto>> recruiterCandidates(
            @PathVariable Long recruiterId,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) Integer minExperience,
            @RequestParam(required = false) Double minMatchScore) {
        return recruiterCandidateService.recruiterCandidates(recruiterId, skill, minExperience, minMatchScore);
    }

    @GetMapping("/applications/{recruiterId}")
    public List<JobApplicationResponse> recruiterApplications(@PathVariable Long recruiterId) {
        return recruiterApplicationService.recruiterApplications(recruiterId);
    }

    @PatchMapping("/applications/{applicationId}/status")
    public JobApplicationResponse updateApplicationStatus(@PathVariable Long applicationId, @RequestParam String status) {
        return recruiterApplicationService.updateStatus(applicationId, status);
    }
}

