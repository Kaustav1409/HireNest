package com.hirenest.backend.dto;

import java.util.List;
import java.util.Map;

public class AiDtos {

    public static class ChatRequest {
        public Long userId;
        public String message;
    }

    public static class ChatResponse {
        public String aiReply;
        /** e.g. JOBS, MISSING_SKILLS, GENERAL */
        public String intent;
        /** Optional snippets: topJobs, missingSkills, quizSummary, hints */
        public Map<String, Object> supportingData;
    }

    /** Aggregated job-seeker state for prompt building and templated replies. */
    public static class JobSeekerAiContext {
        public String preferredRoles;
        public String skillsSummary;
        public int profileCompletionPercent;
        public List<String> missingProfileItems;
        public double latestQuizScore;
        public int quizAttemptCount;
        public double avgQuizScore;
        public int matchedJobCount;
        public List<JobMatchSnippet> topMatches;
        public List<String> aggregatedMissingSkills;
        public String topMissingForLearning;
    }

    public static class JobMatchSnippet {
        public String title;
        public String companyName;
        public Double matchScore;
        public String matchLabel;
        public List<String> missingSkills;
    }
}

