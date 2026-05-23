package com.hirenest.backend.util;

import com.hirenest.backend.entity.CandidateProfile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ProfileCompletionUtil {

    private ProfileCompletionUtil() {
    }

    public static boolean hasResumeOnFile(CandidateProfile profile) {
        if (profile == null) {
            return false;
        }
        String path = profile.getResumePath();
        if (path != null && !path.isBlank()) {
            return true;
        }
        String name = profile.getResumeFileName();
        return name != null && !name.isBlank();
    }

    public static Map<String, Object> build(CandidateProfile profile, boolean quizAttempted) {
        List<String> missing = new ArrayList<>();
        if (profile == null) {
            profile = new CandidateProfile();
        }
        boolean hasSkills = profile.getSkills() != null && !profile.getSkills().isBlank();
        boolean hasBio = profile.getBio() != null && !profile.getBio().isBlank();
        boolean hasExperience = profile.getExperienceYears() != null && profile.getExperienceYears() > 0;
        boolean hasResume = hasResumeOnFile(profile);
        boolean hasPreferredRoles = profile.getPreferredRoles() != null && !profile.getPreferredRoles().isBlank();
        boolean hasLocation = profile.getLocation() != null && !profile.getLocation().isBlank();
        boolean hasLocationOrRemote = hasLocation || Boolean.TRUE.equals(profile.getRemotePreferred());

        int done = 0;
        if (hasSkills) {
            done++;
        } else {
            missing.add("Add your skills");
        }
        if (hasBio) {
            done++;
        } else {
            missing.add("Add a short bio");
        }
        if (hasExperience) {
            done++;
        } else {
            missing.add("Add years of experience");
        }
        if (hasResume) {
            done++;
        } else {
            missing.add("Upload your resume");
        }
        if (quizAttempted) {
            done++;
        } else {
            missing.add("Attempt assessment quiz");
        }
        if (hasPreferredRoles) {
            done++;
        } else {
            missing.add("Set preferred roles");
        }
        if (hasLocationOrRemote) {
            done++;
        } else {
            missing.add("Set location or remote preference");
        }

        Map<String, Object> out = new HashMap<>();
        out.put("profileCompletionPercentage", (int) Math.round((double) done / 7.0 * 100.0));
        out.put("missingProfileItems", missing);
        return out;
    }
}
