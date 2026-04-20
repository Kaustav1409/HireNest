package com.hirenest.backend.service;

import com.hirenest.backend.dto.JobDtos.MatchedJobResponse;
import com.hirenest.backend.dto.ProfileDtos.CandidateProfileResponse;
import com.hirenest.backend.entity.Job;
import com.hirenest.backend.entity.QuizAttempt;
import com.hirenest.backend.entity.CandidateProfile;
import com.hirenest.backend.repository.JobApplicationRepository;
import com.hirenest.backend.repository.JobRepository;
import com.hirenest.backend.repository.QuizAttemptRepository;
import com.hirenest.backend.repository.SavedJobRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ArrayList;
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

    public DashboardService(MatchingService matchingService, ProfileService profileService,
                            JobService jobService, RecruiterApplicationService recruiterApplicationService,
                            JobRepository jobRepository, SavedJobRepository savedJobRepository,
                            QuizAttemptRepository quizAttemptRepository,
                            JobApplicationRepository jobApplicationRepository,
                            DashboardAiInsightsService dashboardAiInsightsService) {
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
        Map<String, Object> data = new HashMap<>();
        List<QuizAttempt> attempts = quizAttemptRepository.findTop5ByUserIdOrderByAttemptedAtDesc(userId);
        CandidateProfile profile = profileService.getCandidateOrDefault(userId);
        Map<String, Object> completion = buildProfileCompletion(profile, !attempts.isEmpty());
        List<MatchedJobResponse> recommended = matchingService.matching(userId);
        data.put("recommendedJobs", recommended);
        CandidateProfileResponse profileView = profileService.toCandidateProfileResponse(userId, profile);
        data.put("profile", profileView);
        data.put("quizAttempts", attempts);
        data.put("assessmentScore", attempts.isEmpty() ? 0.0 : attempts.get(0).getScore());
        data.put("totalJobs", jobRepository.count());
        data.put("matchedJobsCount", recommended.size());
        data.put("savedJobsCount", savedJobRepository.findByUserId(userId).size());
        data.put("totalApplications", jobApplicationRepository.countByUserId(userId));
        data.put("profileCompletionPercentage", completion.get("profileCompletionPercentage"));
        data.put("missingProfileItems", completion.get("missingProfileItems"));
        int pct = completion.get("profileCompletionPercentage") instanceof Integer i ? i : 0;
        @SuppressWarnings("unchecked")
        List<String> missingItems = (List<String>) completion.get("missingProfileItems");
        data.put("aiInsights", dashboardAiInsightsService.buildJobSeekerInsights(
                profile, recommended, attempts, pct, missingItems == null ? List.of() : missingItems));
        return data;
    }

    public Map<String, Object> recruiterDashboard(Long recruiterId) {
        Map<String, Object> data = new HashMap<>();
        List<Job> jobs = jobService.recruiterJobs(recruiterId);
        data.put("totalPostedJobs", jobs.size());
        data.put("jobsPosted", jobs.size());
        data.put("totalCandidates", recruiterApplicationService.recruiterApplications(recruiterId).size());
        data.put("jobs", jobs);
        return data;
    }

    public Map<String, Object> jobSeekerInsights(Long userId) {
        Map<String, Object> out = new HashMap<>();
        List<QuizAttempt> attempts = quizAttemptRepository.findByUserId(userId);
        double avgQuizScore = attempts.isEmpty() ? 0.0 :
                attempts.stream().mapToDouble(a -> a.getScore() == null ? 0.0 : a.getScore()).average().orElse(0.0);
        out.put("attemptCount", attempts.size());
        out.put("avgQuizScore", round2(avgQuizScore));
        out.put("bestQuizScore", round2(attempts.stream()
                .map(a -> a.getScore() == null ? 0.0 : a.getScore())
                .max(Comparator.naturalOrder()).orElse(0.0)));
        out.put("recommendedCount", matchingService.matching(userId).size());
        return out;
    }

    public Map<String, Object> recruiterInsights(Long recruiterId) {
        Map<String, Object> out = new HashMap<>();
        List<Job> jobs = jobService.recruiterJobs(recruiterId);
        out.put("jobsPosted", jobs.size());
        out.put("totalJobs", jobs.size());
        out.put("totalApplicants", recruiterApplicationService.recruiterApplications(recruiterId).size());
        out.put("shortlistedCandidates", recruiterApplicationService.countByStatus(recruiterId, "SHORTLISTED"));
        int totalRequiredSkills = jobs.stream()
                .map(Job::getRequiredSkills)
                .filter(s -> s != null && !s.isBlank())
                .mapToInt(s -> s.split(",").length)
                .sum();
        out.put("avgSkillsPerJob", jobs.isEmpty() ? 0.0 : round2((double) totalRequiredSkills / jobs.size()));
        out.put("openPositions", jobs.size());
        out.put("appliedCount", recruiterApplicationService.countByStatus(recruiterId, "APPLIED"));
        out.put("shortlistedCount", recruiterApplicationService.countByStatus(recruiterId, "SHORTLISTED"));
        out.put("rejectedCount", recruiterApplicationService.countByStatus(recruiterId, "REJECTED"));
        out.put("aiInsights", dashboardAiInsightsService.buildRecruiterInsights(recruiterId));
        return out;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Map<String, Object> buildProfileCompletion(CandidateProfile profile, boolean quizAttempted) {
        List<String> missing = new ArrayList<>();

        boolean hasSkills = profile.getSkills() != null && !profile.getSkills().isBlank();
        boolean hasBio = profile.getBio() != null && !profile.getBio().isBlank();
        boolean hasExperience = profile.getExperienceYears() != null && profile.getExperienceYears() > 0;
        boolean hasResume = profile.getResumePath() != null && !profile.getResumePath().isBlank();
        boolean hasPreferredRoles = profile.getPreferredRoles() != null && !profile.getPreferredRoles().isBlank();
        boolean hasLocation = profile.getLocation() != null && !profile.getLocation().isBlank();
        boolean hasLocationOrRemote = hasLocation || Boolean.TRUE.equals(profile.getRemotePreferred());

        int done = 0;
        if (hasSkills) done++;
        else missing.add("Add your skills");
        if (hasBio) done++;
        else missing.add("Add a short bio");
        if (hasExperience) done++;
        else missing.add("Add years of experience");
        if (hasResume) done++;
        else missing.add("Upload your resume");
        if (quizAttempted) done++;
        else missing.add("Attempt assessment quiz");
        if (hasPreferredRoles) done++;
        else missing.add("Set preferred roles");
        if (hasLocationOrRemote) done++;
        else missing.add("Set location or remote preference");

        Map<String, Object> out = new HashMap<>();
        out.put("profileCompletionPercentage", (int) Math.round((done / 7.0) * 100));
        out.put("missingProfileItems", missing);
        return out;
    }
}

