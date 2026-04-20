package com.hirenest.backend.service;

import com.hirenest.backend.dto.AiDtos.JobMatchSnippet;
import com.hirenest.backend.dto.AiDtos.JobSeekerAiContext;
import com.hirenest.backend.dto.JobDtos.MatchedJobResponse;
import com.hirenest.backend.entity.CandidateProfile;
import com.hirenest.backend.entity.QuizAttempt;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Collects job seeker dashboard data into a single context object for the AI assistant.
 */
@Service
public class PromptBuilderService {

    private final DashboardService dashboardService;
    private final ProfileService profileService;

    public PromptBuilderService(DashboardService dashboardService, ProfileService profileService) {
        this.dashboardService = dashboardService;
        this.profileService = profileService;
    }

    @SuppressWarnings("unchecked")
    public JobSeekerAiContext buildJobSeekerContext(Long userId) {
        Map<String, Object> dash = dashboardService.jobSeekerDashboard(userId);
        Map<String, Object> insights = dashboardService.jobSeekerInsights(userId);

        CandidateProfile profile = profileService.getCandidateOrDefault(userId);
        List<MatchedJobResponse> jobs = (List<MatchedJobResponse>) dash.get("recommendedJobs");
        if (jobs == null) {
            jobs = List.of();
        }

        List<QuizAttempt> attempts = (List<QuizAttempt>) dash.get("quizAttempts");
        if (attempts == null) {
            attempts = List.of();
        }

        JobSeekerAiContext ctx = new JobSeekerAiContext();
        ctx.preferredRoles = profile != null && profile.getPreferredRoles() != null ? profile.getPreferredRoles() : "";
        ctx.skillsSummary = profile != null && profile.getSkills() != null ? profile.getSkills() : "";
        ctx.profileCompletionPercent = dash.get("profileCompletionPercentage") instanceof Integer i ? i : 0;
        ctx.missingProfileItems = (List<String>) dash.get("missingProfileItems");
        if (ctx.missingProfileItems == null) {
            ctx.missingProfileItems = List.of();
        }

        ctx.latestQuizScore = dash.get("assessmentScore") instanceof Number n ? n.doubleValue() : 0.0;
        ctx.quizAttemptCount = attempts.size();
        ctx.avgQuizScore = insights.get("avgQuizScore") instanceof Number n ? n.doubleValue() : 0.0;
        ctx.matchedJobCount = jobs.size();

        ctx.topMatches = jobs.stream().limit(5).map(this::toSnippet).collect(Collectors.toList());
        ctx.aggregatedMissingSkills = aggregateMissingSkills(jobs);
        ctx.topMissingForLearning = ctx.aggregatedMissingSkills.isEmpty()
                ? ""
                : String.join(", ", ctx.aggregatedMissingSkills.stream().limit(5).toList());

        return ctx;
    }

    private JobMatchSnippet toSnippet(MatchedJobResponse j) {
        JobMatchSnippet s = new JobMatchSnippet();
        s.title = j.title;
        s.companyName = j.companyName;
        s.matchScore = j.matchScore;
        s.matchLabel = j.matchLabel;
        s.missingSkills = j.missingSkills != null ? j.missingSkills : List.of();
        return s;
    }

    private List<String> aggregateMissingSkills(List<MatchedJobResponse> jobs) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (MatchedJobResponse j : jobs) {
            if (j.missingSkills == null) continue;
            for (String m : j.missingSkills) {
                if (m != null && !m.trim().isBlank()) {
                    seen.add(m.trim());
                }
            }
        }
        return new ArrayList<>(seen);
    }
}

