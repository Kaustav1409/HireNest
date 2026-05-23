package com.hirenest.backend.dto;

import java.util.List;
import java.util.Map;

public class JobDtos {

    public static class SaveJobRequest {
        public Long userId;
        public Long jobId;
    }

    public static class MatchingFilterRequest {
        public Double minSalary;
        public Double maxSalary;
        public String company;
        public Boolean remote;
        public String location;
        public String keyword;
    }

    public static class LearningResourceDto {
        public String title;
        public String url;
        public String type;

        public LearningResourceDto() {
        }

        public LearningResourceDto(String title, String url, String type) {
            this.title = title;
            this.url = url;
            this.type = type;
        }
    }

    public static class LearningRoadmapDto {
        public String skillName;
        public List<LearningResourceDto> beginnerResources;
        public List<LearningResourceDto> intermediateResources;
        public List<LearningResourceDto> advancedResources;
    }

    public static class LearningRecommendation {
        public String skillName;
        public String beginnerLink;
        public String intermediateLink;
        public String advancedLink;
        public String primaryLink;
        public String resourceType;
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
        public Map<String, String> learningLinks;
        public String explanation;
        public String gapSummary;
        public List<LearningRecommendation> learningRecommendations;
        public List<LearningRoadmapDto> learningRoadmaps;
        public Boolean platformJob;
        public String postedByLabel;
        public String experienceLevel;
        public String category;
        public String roleType;
    }

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
        public String experienceLevel;
        public String category;
        public String roleType;
    }
}
