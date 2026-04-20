package com.hirenest.backend.dto;

import java.util.List;

public class RecruiterDtos {
    /**
     * Candidate ranked against one job: match %, overlap count, quiz and resume signals.
     */
    public static class RankedCandidateDto {
        public Long userId;
        public String name;
        public String skills;
        public Integer experienceYears;
        /** Percentage overlap with job required skills (same formula as matching). */
        public Double matchScore;
        /** Count of required skills the candidate has. */
        public Integer skillsOverlapCount;
        /** Total required skills defined on the job (for X of Y phrasing). */
        public Integer jobRequiredSkillCount;
        /** Required skills the candidate does not yet show (normalized tokens). */
        public List<String> missingSkills;
        /** Short AI-style narrative for recruiter review (generated from profile + fit data). */
        public String candidateSummary;
        /** Most recent quiz score (%), or null if none. */
        public Double latestQuizScorePercent;
        public Boolean resumeUploaded;
        /** Relative API path to download resume, or null. */
        public String resumeDownloadUrl;
    }
}

