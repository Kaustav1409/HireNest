package com.hirenest.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

public class ProfileDtos {
    /**
     * JSON body for POST /api/profile/candidate. Uses a record so Jackson binds
     * reliably (public-field-only classes can fail deserialization under default visibility).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record CandidateProfileRequest(
            Long userId,
            String skills,
            String preferredRoles,
            String location,
            Boolean remotePreferred,
            Integer experienceYears,
            String experienceLevel,
            String bio) {
    }

    /**
     * Stable JSON shape for dashboard and GET /api/profile/candidate/{userId}. Built without mutating the
     * JPA entity (avoids accidental dirty state under open-session-in-view).
     */
    public static record CandidateProfileResponse(
            Long id,
            Long userId,
            String skills,
            String extractedSkills,
            Integer experienceYears,
            String experienceLevel,
            String bio,
            String preferredRoles,
            String location,
            boolean remotePreferred,
            String resumeFileName,
            LocalDateTime resumeUploadedAt) {}

    public static class RecruiterProfileRequest {
        public Long userId;
        public String companyName;
        public String companyDescription;
    }
}

