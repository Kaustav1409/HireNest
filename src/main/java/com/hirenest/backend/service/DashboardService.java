/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.hirenest.backend.service;

import com.hirenest.backend.dto.JobDtos;
import com.hirenest.backend.dto.ProfileDtos;
import com.hirenest.backend.entity.CandidateProfile;
import com.hirenest.backend.entity.Job;
import com.hirenest.backend.entity.QuizAttempt;
import com.hirenest.backend.repository.JobApplicationRepository;
import com.hirenest.backend.repository.JobRepository;
import com.hirenest.backend.repository.QuizAttemptRepository;
import com.hirenest.backend.repository.SavedJobRepository;
import com.hirenest.backend.util.ProfileCompletionUtil;
import com.hirenest.backend.service.DashboardAiInsightsService;
import com.hirenest.backend.service.JobService;
import com.hirenest.backend.service.MatchingService;
import com.hirenest.backend.service.ProfileService;
import com.hirenest.backend.service.RecruiterApplicationService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final MatchingService matchingService;
    private final ProfileService profileService;
    private final JobService jobService;
    private final RecruiterApplicationService recruiterApplicationService;
    private final JobRepository jobRepository;
    private final SavedJobRepository savedJobRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final DashboardAiInsightsService dashboardAiInsightsService;

    public DashboardService(MatchingService matchingService, ProfileService profileService, JobService jobService, RecruiterApplicationService recruiterApplicationService, JobRepository jobRepository, SavedJobRepository savedJobRepository, QuizAttemptRepository quizAttemptRepository, JobApplicationRepository jobApplicationRepository, DashboardAiInsightsService dashboardAiInsightsService) {
        this.matchingService = matchingService;
        this.profileService = profileService;
        this.jobService = jobService;
        this.recruiterApplicationService = recruiterApplicationService;
        this.jobRepository = jobRepository;
        this.savedJobRepository = savedJobRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.dashboardAiInsightsService = dashboardAiInsightsService;
    }

    public Map<String, Object> jobSeekerDashboard(Long userId) {
        int n;
        HashMap<String, Object> data = new HashMap<String, Object>();
        List<QuizAttempt> attempts = this.quizAttemptRepository.findTop5ByUserIdOrderByAttemptedAtDesc(userId);
        CandidateProfile profile = this.profileService.getCandidateOrDefault(userId);
        Map<String, Object> completion = this.buildProfileCompletion(profile, !attempts.isEmpty());
        List<JobDtos.MatchedJobResponse> recommended = this.matchingService.matching(userId);
        data.put("recommendedJobs", recommended);
        ProfileDtos.CandidateProfileResponse profileView = this.profileService.toCandidateProfileResponse(userId, profile);
        data.put("profile", profileView);
        data.put("quizAttempts", attempts);
        data.put("assessmentScore", attempts.isEmpty() ? 0.0 : attempts.get(0).getScore());
        data.put("totalJobs", this.jobRepository.count());
        data.put("matchedJobsCount", recommended.size());
        data.put("savedJobsCount", this.savedJobRepository.findByUserId(userId).size());
        data.put("totalApplications", this.jobApplicationRepository.countByUserId(userId));
        data.put("profileCompletionPercentage", completion.get("profileCompletionPercentage"));
        data.put("missingProfileItems", completion.get("missingProfileItems"));
        Object object = completion.get("profileCompletionPercentage");
        if (object instanceof Integer) {
            Integer i = (Integer)object;
            n = i;
        } else {
            n = 0;
        }
        int pct = n;
        List missingItems = (List)completion.get("missingProfileItems");
        data.put("aiInsights", this.dashboardAiInsightsService.buildJobSeekerInsights(profile, recommended, attempts, pct, missingItems == null ? List.of() : missingItems));
        return data;
    }

    public Map<String, Object> recruiterDashboard(Long recruiterId) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        List<Job> jobs = this.jobService.recruiterJobs(recruiterId);
        data.put("totalPostedJobs", jobs.size());
        data.put("jobsPosted", jobs.size());
        data.put("totalCandidates", this.recruiterApplicationService.recruiterApplications(recruiterId).size());
        data.put("jobs", jobs);
        return data;
    }

    public Map<String, Object> jobSeekerInsights(Long userId) {
        HashMap<String, Object> out = new HashMap<String, Object>();
        List<QuizAttempt> attempts = this.quizAttemptRepository.findByUserId(userId);
        double avgQuizScore = attempts.isEmpty() ? 0.0 : attempts.stream().mapToDouble(a -> a.getScore() == null ? 0.0 : a.getScore()).average().orElse(0.0);
        out.put("attemptCount", attempts.size());
        out.put("avgQuizScore", this.round2(avgQuizScore));
        out.put("bestQuizScore", this.round2(attempts.stream().map(a -> a.getScore() == null ? 0.0 : a.getScore()).max(Comparator.naturalOrder()).orElse(0.0)));
        out.put("recommendedCount", this.matchingService.matching(userId).size());
        return out;
    }

    public Map<String, Object> recruiterInsights(Long recruiterId) {
        HashMap<String, Object> out = new HashMap<String, Object>();
        List<Job> jobs = this.jobService.recruiterJobs(recruiterId);
        out.put("jobsPosted", jobs.size());
        out.put("totalJobs", jobs.size());
        out.put("totalApplicants", this.recruiterApplicationService.recruiterApplications(recruiterId).size());
        out.put("shortlistedCandidates", this.recruiterApplicationService.countByStatus(recruiterId, "SHORTLISTED"));
        int totalRequiredSkills = jobs.stream().map(Job::getRequiredSkills).filter(s -> s != null && !s.isBlank()).mapToInt(s -> s.split(",").length).sum();
        out.put("avgSkillsPerJob", jobs.isEmpty() ? 0.0 : this.round2((double)totalRequiredSkills / (double)jobs.size()));
        out.put("openPositions", jobs.size());
        out.put("appliedCount", this.recruiterApplicationService.countByStatus(recruiterId, "APPLIED"));
        out.put("shortlistedCount", this.recruiterApplicationService.countByStatus(recruiterId, "SHORTLISTED"));
        out.put("rejectedCount", this.recruiterApplicationService.countByStatus(recruiterId, "REJECTED"));
        out.put("aiInsights", this.dashboardAiInsightsService.buildRecruiterInsights(recruiterId));
        return out;
    }

    private double round2(double value) {
        return (double)Math.round(value * 100.0) / 100.0;
    }

    private Map<String, Object> buildProfileCompletion(CandidateProfile profile, boolean quizAttempted) {
        return ProfileCompletionUtil.build(profile, quizAttempted);
    }
}
