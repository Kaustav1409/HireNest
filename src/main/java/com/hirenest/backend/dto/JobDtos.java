package com.hirenest.backend.dto;

import java.util.List;
import java.util.Map;

public class JobDtos {
    public static class JobRequest {
        public Long recruiterId;
        public String title;
        public String companyName;
        public String description;
        public String requiredSkills;
        public String location;
        public Boolean remote;
        public Double salaryMin;
        public Double salaryMax;
    }

    public static class MatchedJobResponse {
        public Long jobId;
        public String title;
        public String companyName;
        public String location;
        public Boolean remote;
        public Double minSalary;
        public Double maxSalary;
        public Double matchScore;
        public Double rankingScore;
        public String matchLabel;
        public List<String> matchedSkills;
        public List<String> missingSkills;
        /** Legacy flat map: skill -> primary link (kept for compatibility). */
        public Map<String, String> learningLinks;
        /** Human-readable summary of overlap and gaps. */
        public String explanation;
        /** Skill gap snapshot for quick reading. */
        public String gapSummary;
        /** Structured recommendations per missing skill. */
        public List<LearningRecommendation> learningRecommendations;
        /** Structured roadmap per missing skill (Beginner/Intermediate/Advanced). */
        public List<LearningRoadmapDto> learningRoadmaps;
    }

    public static class LearningRecommendation {
        public String skillName;
        public String beginnerLink;
        public String intermediateLink;
        public String advancedLink;
        public String primaryLink;
        public String resourceType;
    }

    /** Per missing skill: three tiers of curated or fallback resources. */
    public static class LearningRoadmapDto {
        public String skillName;
        public List<LearningResourceDto> beginnerResources;
        public List<LearningResourceDto> intermediateResources;
        public List<LearningResourceDto> advancedResources;
    }

    public static class LearningResourceDto {
        public String title;
        public String url;
        public String type; // course/documentation/project/video/certification

        public LearningResourceDto() {}

        public LearningResourceDto(String title, String url, String type) {
            this.title = title;
            this.url = url;
            this.type = type;
        }
    }

    public static class MatchingFilterRequest {
        public Double minSalary;
        public Double maxSalary;
        public String company;
        public Boolean remote;
        public String location;
        public String keyword;
    }

    public static class SaveJobRequest {
        public Long userId;
        public Long jobId;
    }
}

