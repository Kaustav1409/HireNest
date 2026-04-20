package com.hirenest.backend.service;

import com.hirenest.backend.dto.JobDtos.LearningResourceDto;
import com.hirenest.backend.dto.JobDtos.LearningRoadmapDto;
import com.hirenest.backend.dto.QuizDtos.QuizLearningResource;
import com.hirenest.backend.dto.QuizDtos.QuizResultResponse;
import com.hirenest.backend.util.LearningResourceMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Connects quiz outcomes to the platform learning catalog ({@link LearningResourceMapper}) without
 * changing matching or skill-gap APIs.
 */
@Service
public class QuizLearningIntegrationService {

    public void attachLearningFollowUp(QuizResultResponse r) {
        if (r == null || r.skill == null || r.skill.isBlank() || "general".equals(r.skill)) {
            return;
        }
        double score = r.score != null ? r.score : 0.0;
        String slug = r.skill;

        r.topicsToImprove = buildTopicsToImprove(slug, score);
        r.learningResources = buildLearningResources(slug, score);
        r.nextLearningSuggestion = buildNextLearningSuggestion(slug, score);
        r.roleFitDirection = buildRoleFitDirection(slug, score);
        r.nextAssessmentHint = buildNextAssessmentHint(slug, score, r.difficulty);
    }

    /** Maps quiz domain slugs to keys understood by {@link LearningResourceMapper}. */
    private String mapSkillForRoadmap(String slug) {
        return switch (slug) {
            case "dbms" -> "sql";
            case "web development" -> "javascript";
            case "data structures" -> "java";
            case "aptitude" -> "aptitude";
            default -> slug;
        };
    }

    private List<String> buildTopicsToImprove(String slug, double score) {
        List<String> topics = new ArrayList<>();
        if (score >= 85) {
            topics.add("Advanced patterns and real-world edge cases");
            topics.add("Performance, testing, and maintainability");
            return topics;
        }
        switch (slug) {
            case "java" -> topics.addAll(List.of(
                    "Object-oriented design and classes",
                    "Collections, generics, and APIs",
                    "Exceptions and debugging"));
            case "python" -> topics.addAll(List.of(
                    "Functions, modules, and packages",
                    "Data structures and idioms",
                    "Error handling and virtual environments"));
            case "javascript" -> topics.addAll(List.of(
                    "Types, scope, and strict mode",
                    "Async flows (promises/async-await)",
                    "DOM and modern ES features"));
            case "react" -> topics.addAll(List.of(
                    "Components, props, and state",
                    "Hooks and side effects",
                    "Performance and patterns"));
            case "spring boot" -> topics.addAll(List.of(
                    "Configuration and beans",
                    "REST controllers and validation",
                    "Data access and security basics"));
            case "sql", "dbms" -> topics.addAll(List.of(
                    "JOINs, GROUP BY, and subqueries",
                    "Indexing and normalization",
                    "Transactions and constraints"));
            case "data structures" -> topics.addAll(List.of(
                    "Arrays, lists, stacks, queues",
                    "Trees and graphs (basics)",
                    "Time and space complexity"));
            case "aptitude" -> topics.addAll(List.of(
                    "Percentages and ratios",
                    "Time/speed/distance",
                    "Logical and numerical patterns"));
            case "web development" -> topics.addAll(List.of(
                    "HTTP, REST, and status codes",
                    "Security basics (CORS, XSS, CSP)",
                    "HTML/CSS/JS integration"));
            default -> topics.add("Core syntax and fundamentals");
        }
        if (score >= 50 && score < 85) {
            topics.add("Hands-on practice and small projects");
        }
        int max = Math.min(4, topics.size());
        return new ArrayList<>(topics.subList(0, max));
    }

    private List<QuizLearningResource> buildLearningResources(String slug, double score) {
        String key = mapSkillForRoadmap(slug);
        LearningRoadmapDto roadmap = LearningResourceMapper.roadmapFor(key);
        List<LearningResourceDto> picked = pickTierResources(roadmap, score);
        List<QuizLearningResource> out = new ArrayList<>();
        for (LearningResourceDto dto : picked) {
            if (dto == null || dto.url == null || dto.url.isBlank()) {
                continue;
            }
            QuizLearningResource q = new QuizLearningResource();
            q.title = dto.title != null ? dto.title : "Learning resource";
            q.url = dto.url;
            out.add(q);
            if (out.size() >= 3) {
                break;
            }
        }
        if (out.isEmpty()) {
            QuizLearningResource q = new QuizLearningResource();
            q.title = "Learn " + formatSkillTitle(slug);
            q.url = LearningResourceMapper.linkFor(key);
            out.add(q);
        }
        return out;
    }

    private List<LearningResourceDto> pickTierResources(LearningRoadmapDto rm, double score) {
        List<LearningResourceDto> acc = new ArrayList<>();
        if (rm == null) {
            return acc;
        }
        if (score < 50) {
            addAllNonNull(acc, rm.beginnerResources);
            addFirstNonNull(acc, rm.intermediateResources);
        } else if (score < 80) {
            addAllNonNull(acc, rm.intermediateResources);
            addFirstNonNull(acc, rm.advancedResources);
        } else {
            addAllNonNull(acc, rm.advancedResources);
            addFirstNonNull(acc, rm.intermediateResources);
        }
        return dedupeByUrl(acc, 5);
    }

    private void addAllNonNull(List<LearningResourceDto> acc, List<LearningResourceDto> src) {
        if (src == null) {
            return;
        }
        for (LearningResourceDto d : src) {
            if (d != null && d.url != null && !d.url.isBlank()) {
                acc.add(d);
            }
        }
    }

    private void addFirstNonNull(List<LearningResourceDto> acc, List<LearningResourceDto> src) {
        if (src == null || src.isEmpty()) {
            return;
        }
        LearningResourceDto d = src.get(0);
        if (d != null && d.url != null && !d.url.isBlank()) {
            acc.add(d);
        }
    }

    private List<LearningResourceDto> dedupeByUrl(List<LearningResourceDto> in, int max) {
        Set<String> seen = new LinkedHashSet<>();
        List<LearningResourceDto> out = new ArrayList<>();
        for (LearningResourceDto d : in) {
            if (d == null || d.url == null) {
                continue;
            }
            if (seen.add(d.url) && out.size() < max) {
                out.add(d);
            }
        }
        return out;
    }

    private String buildNextLearningSuggestion(String slug, double score) {
        String label = formatSkillTitle(slug);
        if (score < 50) {
            return "Start with the beginner-oriented resources below, practice daily, then retake this "
                    + label
                    + " assessment.";
        }
        if (score < 80) {
            return "Use the intermediate materials to deepen "
                    + label
                    + " skills; combine with the Recommended Jobs skill-gap cards for targeted practice.";
        }
        return "Strong "
                + label
                + " signal â€” explore advanced resources and highlight this strength in applications.";
    }

    private String buildRoleFitDirection(String slug, double score) {
        boolean strong = score >= 75;
        return switch (slug) {
            case "sql", "dbms" -> strong
                    ? "Your SQL performance suggests backend, data engineering, and analytics-aligned roles."
                    : "Strengthen querying and modeling before targeting data-heavy job descriptions.";
            case "java", "spring boot" -> strong
                    ? "Good fit for backend and enterprise Java roles; keep system design on your radar."
                    : "Focus on core Java and APIs before emphasizing backend titles in applications.";
            case "python" -> strong
                    ? "Strong for scripting, data, and backend Python roles â€” add projects to your profile."
                    : "Build small Python projects to support automation and data-focused listings.";
            case "javascript", "react" -> strong
                    ? "Aligns with frontend and full-stack roles; showcase a portfolio piece."
                    : "Practice UI state and component patterns to match frontend job requirements.";
            case "data structures" -> strong
                    ? "Supports competitive coding and technical interviews for software engineering tracks."
                    : "Drill fundamentals to unlock better interview performance for SWE roles.";
            case "web development" -> strong
                    ? "Broad web literacy helps full-stack and product-adjacent roles."
                    : "Solidify HTTP, security, and deployment basics before claiming full-stack fit.";
            case "aptitude" -> strong
                    ? "Numerical aptitude supports analyst, operations, and many campus hiring pipelines."
                    : "Extra aptitude practice improves screening and assessment rounds.";
            default -> strong
                    ? "Leverage this skill in applications where the job lists related technologies."
                    : "Close skill gaps with the resources below, then revisit role recommendations.";
        };
    }

    private String buildNextAssessmentHint(String slug, double score, String difficulty) {
        String d = difficulty == null ? "" : difficulty.trim();
        if (score < 55 && d.isEmpty()) {
            return "Next: try the Beginner difficulty filter for a focused question pool.";
        }
        if (score < 55 && !d.isEmpty()) {
            return "Next: after studying, retake with the same filter or use All levels for mixed practice.";
        }
        if (score >= 80 && d.isEmpty()) {
            return "Next: try Intermediate or Advanced difficulty for a harder quiz in this domain.";
        }
        if (score >= 80 && "Beginner".equalsIgnoreCase(d)) {
            return "Next: move to Intermediate difficulty or another domain to diversify.";
        }
        if (score >= 80) {
            return "Next: explore another domain (e.g. "
                    + suggestAdjacentDomain(slug)
                    + ") to broaden your profile.";
        }
        return "Next: compare your Recommended Jobs skill gaps with this quiz domain.";
    }

    private String suggestAdjacentDomain(String slug) {
        return switch (slug) {
            case "java" -> "Spring Boot or SQL";
            case "spring boot" -> "Java or SQL";
            case "sql", "dbms" -> "Backend stack (Java or Spring Boot)";
            case "react" -> "JavaScript or web development";
            case "javascript" -> "React or web development";
            default -> "SQL or Java";
        };
    }

    private String formatSkillTitle(String slug) {
        if (slug == null || slug.isBlank()) {
            return "Skill";
        }
        String[] parts = slug.trim().toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1));
            }
        }
        return sb.toString();
    }
}

