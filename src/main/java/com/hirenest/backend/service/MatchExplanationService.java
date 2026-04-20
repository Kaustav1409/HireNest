package com.hirenest.backend.service;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Builds human-readable match explanations from existing matching outputs (scores and skill lists).
 * Deterministic, template-based text â€” no external LLM â€” suitable for demo and consistent API responses.
 */
@Service
public class MatchExplanationService {

    /**
     * @param matchScore      percentage 0â€“100 (already rounded by caller if needed)
     * @param matchedSkills   skills the candidate has that the job requires
     * @param missingSkills   required skills not yet on the candidate profile
     * @param jobTitle        job title for phrasing; may be null
     */
    public String generateExplanation(double matchScore, List<String> matchedSkills, List<String> missingSkills, String jobTitle) {
        String rolePhrase = (jobTitle == null || jobTitle.isBlank()) ? "this role" : "\"" + jobTitle.trim() + "\"";

        if (missingSkills.isEmpty() && !matchedSkills.isEmpty()) {
            return "You match this role because you already have "
                    + formatSkillList(matchedSkills) + ". Your profile aligns with the core skills required.";
        }

        if (matchedSkills.isEmpty()) {
            if (missingSkills.isEmpty()) {
                return "This job has no required skills defined, so we could not compute a skill match for " + rolePhrase + ".";
            }
            return "Your profile does not yet show overlap with the required skills for " + rolePhrase + ". "
                    + "Focus on " + formatSkillList(missingSkills) + " to become competitive for this position.";
        }

        String matchedPhrase = formatSkillList(matchedSkills);
        String missingPhrase = formatSkillList(missingSkills);

        if (matchScore >= 70.0) {
            return "You match " + rolePhrase + " well because you already have " + matchedPhrase + ". "
                    + "Improving " + missingPhrase + " will further increase your fit.";
        }

        if (matchScore >= 40.0) {
            return "You partially match " + rolePhrase + ". You have " + matchedPhrase + ", but improving "
                    + missingPhrase + " will increase your fit.";
        }

        return "You have some overlap with " + rolePhrase + " (" + matchedPhrase + "), but several requirements are still open. "
                + "Prioritize " + missingPhrase + " to strengthen your profile for this job.";
    }

    /** English list: "a", "a and b", "a, b, and c". */
    private static String formatSkillList(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return "";
        }
        if (skills.size() == 1) {
            return skills.get(0);
        }
        if (skills.size() == 2) {
            return skills.get(0) + " and " + skills.get(1);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < skills.size(); i++) {
            if (i > 0) {
                sb.append(i == skills.size() - 1 ? ", and " : ", ");
            }
            sb.append(skills.get(i));
        }
        return sb.toString();
    }
}

