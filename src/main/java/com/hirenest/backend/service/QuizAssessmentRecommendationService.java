package com.hirenest.backend.service;

import com.hirenest.backend.dto.QuizDtos.QuizRecommendedAssessment;
import com.hirenest.backend.entity.CandidateProfile;
import com.hirenest.backend.util.QuizDomainTaxonomy;
import com.hirenest.backend.util.QuizSkillNormalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Builds ranked assessment recommendations from merged profile + resume skills and domain taxonomy scoring.
 */
@Service
public class QuizAssessmentRecommendationService {

    /** Each canonical skill matched against a domain taxonomy counts as this many points. */
    private static final int POINTS_PER_MATCHED_SKILL = 2;

    private static final int MAX_RECOMMENDATIONS = 12;
    private static final int FALLBACK_APTITUDE_SCORE = 2;
    /** Below this total skill-only points on the best domain, taxonomy match is treated as weak -> suggest Aptitude too. */
    private static final int STRONG_SKILL_MATCH_POINTS = 4;

    public List<QuizRecommendedAssessment> recommend(CandidateProfile profile, List<String> allowedDomainSlugs) {
        LinkedHashSet<String> allowed = new LinkedHashSet<>();
        for (String s : allowedDomainSlugs) {
            if (s != null && !s.isBlank()) {
                allowed.add(s.trim().toLowerCase(Locale.ROOT));
            }
        }

        LinkedHashSet<String> userCanonical = QuizSkillNormalizer.canonicalSkillSetFromProfile(profile);
        boolean hadSkillText = QuizSkillNormalizer.hasSkillText(profile);

        record Row(String slug, int skillPoints, LinkedHashSet<String> matchedCanonical) {}

        List<Row> rows = new ArrayList<>();
        for (String slug : allowed) {
            Set<String> tax = QuizDomainTaxonomy.canonicalSkillsForDomain(slug);
            if (tax.isEmpty()) {
                continue;
            }
            LinkedHashSet<String> matched = new LinkedHashSet<>();
            for (String c : userCanonical) {
                if (tax.contains(c)) {
                    matched.add(c);
                }
            }
            int skillPoints = matched.size() * POINTS_PER_MATCHED_SKILL;
            if (skillPoints > 0) {
                rows.add(new Row(slug, skillPoints, matched));
            }
        }

        rows.sort(
                Comparator.comparingInt((Row r) -> r.skillPoints)
                        .reversed()
                        .thenComparing(r -> r.slug));

        int maxSkillPointsAcrossDomains = rows.stream().mapToInt(r -> r.skillPoints).max().orElse(0);

        List<QuizRecommendedAssessment> out = new ArrayList<>();
        for (int i = 0; i < rows.size() && i < MAX_RECOMMENDATIONS; i++) {
            Row r = rows.get(i);
            int score = r.skillPoints;
            List<String> matchedLabels = QuizSkillNormalizer.labelsForCanonicals(r.matchedCanonical);
            String reason = buildReason(r.slug, r.skillPoints, matchedLabels);
            out.add(
                    new QuizRecommendedAssessment(
                            formatDomainLabel(r.slug),
                            score,
                            List.copyOf(matchedLabels),
                            reason,
                            r.slug));
        }

        if (out.isEmpty()) {
            out.add(
                    new QuizRecommendedAssessment(
                            formatDomainLabel("aptitude"),
                            FALLBACK_APTITUDE_SCORE,
                            List.of(),
                            "No strong domain match from your profile yet - start with Aptitude for general placement readiness.",
                            "aptitude"));
            return out;
        }

        boolean weakTaxonomyMatch =
                !rows.isEmpty() && maxSkillPointsAcrossDomains < STRONG_SKILL_MATCH_POINTS;
        boolean alreadyHasAptitude = out.stream().anyMatch(a -> "aptitude".equalsIgnoreCase(a.skill()));
        if (weakTaxonomyMatch && allowed.contains("aptitude") && !alreadyHasAptitude) {
            String weakReason =
                    hadSkillText && userCanonical.isEmpty()
                            ? "Your listed skills did not map to our specialty taxonomy yet - Aptitude is a good starting point; refine skill names for tighter matches."
                            : "Skill overlap with our specialty quizzes is light - add Aptitude to benchmark reasoning and quantitative basics.";
            out.add(
                    new QuizRecommendedAssessment(
                            formatDomainLabel("aptitude"),
                            FALLBACK_APTITUDE_SCORE,
                            List.of(),
                            weakReason,
                            "aptitude"));
        }

        return out;
    }

    private static String buildReason(
            String slug,
            int skillPoints,
            List<String> matchedLabels) {
        if (!matchedLabels.isEmpty()) {
            String skills = String.join(", ", matchedLabels);
            if ("react".equals(slug)) {
                return "Recommended because React is reflected in your profile/resume skills (" + skills + ").";
            }
            if ("web development".equals(slug)) {
                return "Recommended because your profile/resume strongly matches frontend and web development skills ("
                        + skills
                        + ").";
            }
            return "Recommended because your profile/resume matches: " + skills + ".";
        }
        return "Recommended based on your HireNest profile signals.";
    }

    private static String formatDomainLabel(String normalizedSlug) {
        if (normalizedSlug == null || normalizedSlug.isBlank()) {
            return "";
        }
        String[] parts = normalizedSlug.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            String p = parts[i];
            if (p.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }
}
