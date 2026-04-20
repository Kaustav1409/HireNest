package com.hirenest.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Builds short, deterministic recruiter-facing summaries from candidate + job fit data (no external LLM).
 */
@Service
public class RecruiterCandidateSummaryService {

    /**
     * @param jobTitle            job title for context
     * @param requiredSkillCount  number of required skills on the job
     * @param overlapCount        skills candidate has that match required
     * @param matchedSkillKeys    normalized matched skill tokens (same order as job requirements iteration)
     * @param missingSkillKeys    normalized missing required skills
     * @param experienceYears     candidate years of experience (may be null/0)
     * @param matchScore          0â€“100 overlap percentage
     * @param latestQuizPercent   latest quiz % or null
     * @param resumeUploaded      whether a resume exists
     */
    public String buildSummary(String jobTitle,
                               int requiredSkillCount,
                               int overlapCount,
                               List<String> matchedSkillKeys,
                               List<String> missingSkillKeys,
                               Integer experienceYears,
                               Double matchScore,
                               Double latestQuizPercent,
                               Boolean resumeUploaded) {
        String role = (jobTitle == null || jobTitle.isBlank()) ? "this role" : "\"" + jobTitle.trim() + "\"";

        if (requiredSkillCount <= 0) {
            return "This job does not list required skills in HireNest, so the match score is informational only. "
                    + "Review the candidate's skills and experience manually for " + role + ".";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("This candidate matches ").append(overlapCount).append(" out of ")
                .append(requiredSkillCount).append(" required skills for ").append(role);
        if (matchScore != null) {
            sb.append(" (about ").append(Math.round(matchScore)).append("% skill alignment)");
        }
        sb.append(". ");

        if (!matchedSkillKeys.isEmpty()) {
            sb.append("Demonstrated strengths against this posting include ")
                    .append(formatSkillPhrase(matchedSkillKeys, 4))
                    .append(". ");
        }

        if (!missingSkillKeys.isEmpty()) {
            sb.append("Notable gaps versus the posting: ")
                    .append(formatSkillPhrase(missingSkillKeys, 4))
                    .append(". ");
        }

        int exp = experienceYears == null ? 0 : experienceYears;
        if (exp > 0) {
            sb.append("Experience is listed at ").append(exp).append(exp == 1 ? " year" : " years").append(". ");
        }

        if (Boolean.TRUE.equals(resumeUploaded)) {
            sb.append("A resume is on file for deeper review. ");
        }

        if (latestQuizPercent != null) {
            double q = latestQuizPercent;
            if (q >= 75.0) {
                sb.append("Quiz performance is strong (").append(Math.round(q)).append("%), above typical baseline. ");
            } else if (q >= 55.0) {
                sb.append("Quiz performance is moderate (").append(Math.round(q)).append("%). ");
            } else {
                sb.append("Quiz score is on the lower side (").append(Math.round(q))
                        .append("%); consider technical follow-up. ");
            }
        } else {
            sb.append("No quiz attempt on record yet. ");
        }

        if (matchScore != null && matchScore >= 80.0 && latestQuizPercent != null && latestQuizPercent >= 65.0) {
            sb.append("Overall profile is suitable for shortlist consideration.");
        } else if (matchScore != null && matchScore >= 60.0) {
            sb.append("Worth reviewing against your hiring bar and team needs.");
        } else {
            sb.append("Consider whether upskilling time fits your timeline for this hire.");
        }

        return sb.toString().trim();
    }

    /** First n skills as a readable English list with display-friendly casing. */
    private String formatSkillPhrase(List<String> keys, int max) {
        List<String> pretty = new ArrayList<>();
        int n = Math.min(max, keys.size());
        for (int i = 0; i < n; i++) {
            pretty.add(prettifySkill(keys.get(i)));
        }
        if (keys.size() > max) {
            pretty.add("+" + (keys.size() - max) + " more");
        }
        return joinEnglish(pretty);
    }

    private String prettifySkill(String s) {
        if (s == null || s.isBlank()) return "";
        String t = s.trim().toLowerCase(Locale.ROOT);
        if ("sql".equals(t)) return "SQL";
        if ("html".equals(t)) return "HTML";
        if ("css".equals(t)) return "CSS";
        if ("c++".equals(t) || "cpp".equals(t)) return "C++";
        if ("node.js".equals(t) || "nodejs".equals(t)) return "Node.js";
        String[] parts = t.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isBlank()) continue;
            if (i > 0) out.append(" ");
            String p = parts[i];
            out.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return out.toString();
    }

    private String joinEnglish(List<String> items) {
        if (items.isEmpty()) return "";
        if (items.size() == 1) return items.get(0);
        if (items.size() == 2) return items.get(0) + " and " + items.get(1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(i == items.size() - 1 ? ", and " : ", ");
            }
            sb.append(items.get(i));
        }
        return sb.toString();
    }
}

