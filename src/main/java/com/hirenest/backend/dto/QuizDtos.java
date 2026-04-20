package com.hirenest.backend.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

public class QuizDtos {
    public static class QuizQuestionResponse {
        public Long quizId;
        public String skill;
        public String difficulty;
        public String question;
        public List<String> options;
    }

    public static class QuizSubmission {
        public Long userId;
        public Long attemptId;
        public String skill;
        /** Optional: Beginner / Intermediate / Advanced (echoed on attempt). */
        public String difficulty;
        public Integer durationSeconds;
        public List<Answer> answers;
    }

    public static class Answer {
        public Long quizId;
        public Integer selectedIndex;
    }

    /**
     * Response from {@code POST /api/quiz/submit}. Includes legacy {@link #skillLevel} plus explicit
     * {@link #performanceLevel} and {@link #feedback} for richer UIs.
     */
    public static class QuizResultResponse {
        /** Score percentage, 0â€“100 (two decimal places). */
        public Double score;
        public Integer totalQuestions;
        public Integer correctAnswers;
        public Integer durationSeconds;
        /** Normalized skill/domain slug (e.g. {@code spring boot}). */
        public String skill;
        /** Optional difficulty filter used for the attempt, if any. */
        public String difficulty;
        /**
         * Legacy field: performance tier derived from score (Beginner / Intermediate / Advanced).
         * Same value as {@link #performanceLevel}.
         */
        public String skillLevel;
        /** Performance tier: Beginner (&lt;50%), Intermediate (50â€“79%), Advanced (80%+). */
        public String performanceLevel;
        /** Short, human-readable feedback tailored to score and domain. */
        public String feedback;

        /** Concrete topics to study next (aligned with domain + score). */
        public List<String> topicsToImprove;
        /** Curated links from the same {@link com.hirenest.backend.util.LearningResourceMapper} used for job skill gaps. */
        public List<QuizLearningResource> learningResources;
        /** One-line guidance tying assessment to learning (e.g. which tier of resources to use). */
        public String nextLearningSuggestion;
        /** Career / role direction hint based on domain and score. */
        public String roleFitDirection;
        /** Optional hint for the next quiz attempt (difficulty or domain). */
        public String nextAssessmentHint;
    }

    /** Lightweight link row for quiz result learning follow-up. */
    public static class QuizLearningResource {
        public String title;
        public String url;
    }

    public static class QuizAttemptStartRequest {
        public Long userId;
        public String skill;
        public String difficulty;
    }

    public static class QuizAttemptStartResponse {
        public Long attemptId;
        public Integer heartbeatIntervalSeconds;
        public Integer heartbeatTimeoutSeconds;
        public String status;
    }

    public static class QuizAttemptHeartbeatRequest {
        public Long userId;
        public Long attemptId;
    }

    public static class QuizAttemptViolationRequest {
        public Long userId;
        public Long attemptId;
        public String reason;
    }

    /**
     * One row from {@code GET /api/quiz/recommended/{userId}}.
     * <ul>
     *   <li>{@code domain} â€” display label for the assessment domain</li>
     *   <li>{@code score} â€” strength from matched canonical skills (2 pts each) plus optional preferred-role boosts</li>
     *   <li>{@code matchedSkills} â€” human-readable labels for skills that contributed to the taxonomy match</li>
     *   <li>{@code reason} â€” short explanation for the row</li>
     *   <li>{@code skill} â€” domain slug for {@code GET /api/quiz?skill=} (same values as {@code GET /api/quiz/domains})</li>
     * </ul>
     */
    @JsonPropertyOrder({"domain", "score", "matchedSkills", "reason", "skill"})
    public static record QuizRecommendedAssessment(
            String domain, int score, List<String> matchedSkills, String reason, String skill) {}

    /** Metadata + progress row for assessment setup cards. */
    @JsonPropertyOrder({
            "skill",
            "domain",
            "category",
            "difficulty",
            "questionCount",
            "estimatedMinutes",
            "status",
            "lastScore"
    })
    public static record AssessmentOverviewItem(
            String skill,
            String domain,
            String category,
            String difficulty,
            int questionCount,
            int estimatedMinutes,
            String status,
            Double lastScore) {}

    @JsonPropertyOrder({"skill", "score", "performanceLevel", "completionStatus"})
    public static record UserSkillProfileItem(
            String skill,
            Double score,
            String performanceLevel,
            String completionStatus) {}
}

