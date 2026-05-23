package com.hirenest.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class ApplicationDtos {

    private ApplicationDtos() {
    }

    public static class JobMini {
        public Long id;
        public String title;
        public String companyName;
        public String location;
        public Boolean remote;
        public Boolean platformJob;
        public String postedByLabel;
        public String experienceLevel;
        public String category;
        public String roleType;
    }

    public static class UserMini {
        public Long id;
        public String fullName;
    }

    public static class JobApplicationResponse {
        public Long id;
        public String status;
        public LocalDateTime appliedAt;
        public LocalDateTime shortlistedAt;
        public LocalDateTime rejectedAt;
        public JobMini job;
        public UserMini user;
        public Double applicationStrengthPercent;
        public Double hiringConfidencePercent;
        public String strengthInsights;
        public String githubLink;
        public String portfolioLink;
        public String driveLink;
        public String projectDescription;
        public Boolean resumeOnFile;
        public Boolean projectFileOnFile;
    }

    public static class EnhancedApplyRequest {
        public String githubLink;
        public String portfolioLink;
        public String driveLink;
        public String projectDescription;
        public List<String> interestedRoles;
        public List<String> strongestSkills;
        public String contributionDescription;
        public List<String> availabilityTypes;
    }

    public static class ApplicationStrengthPreview {
        public Double applicationStrengthPercent;
        public Double hiringConfidencePercent;
        public String strengthInsights;
        public Double skillMatchPercent;
        public Boolean resumePresent;
        public Boolean projectPresent;
        public Boolean assessmentAligned;
    }
}
