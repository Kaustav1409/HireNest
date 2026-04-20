package com.hirenest.backend.service;

import com.hirenest.backend.dto.ApplicationDtos.JobApplicationResponse;
import com.hirenest.backend.dto.JobDtos.MatchedJobResponse;
import com.hirenest.backend.dto.RecruiterDtos.RankedCandidateDto;
import com.hirenest.backend.entity.CandidateProfile;
import com.hirenest.backend.entity.Job;
import com.hirenest.backend.entity.QuizAttempt;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Deterministic "AI" insight cards for dashboards (templates over live metrics; no external LLM).
 */
@Service
public class DashboardAiInsightsService {

    private final RecruiterCandidateService recruiterCandidateService;
    private final JobService jobService;
    private final RecruiterApplicationService recruiterApplicationService;

    public DashboardAiInsightsService(RecruiterCandidateService recruiterCandidateService,
                                      JobService jobService,
                                      RecruiterApplicationService recruiterApplicationService) {
        this.recruiterCandidateService = recruiterCandidateService;
        this.jobService = jobService;
        this.recruiterApplicationService = recruiterApplicationService;
    }

    public Map<String, Object> buildJobSeekerInsights(CandidateProfile profile,
                                                       List<MatchedJobResponse> matches,
                                                       List<QuizAttempt> attempts,
                                                       int profileCompletionPercent,
                                                       List<String> missingProfileItems) {
        Map<String, Object> out = new HashMap<>();
        List<MatchedJobResponse> safe = matches == null ? List.of() : matches;

        Map<String, Object> bestFit = new HashMap<>();
        if (!safe.isEmpty()) {
            MatchedJobResponse top = safe.get(0);
            bestFit.put("title", top.title);
            bestFit.put("companyName", top.companyName);
            bestFit.put("matchScore", top.matchScore);
            bestFit.put("matchLabel", top.matchLabel);
        }
        out.put("bestFitRole", bestFit);

        Map<String, Integer> missFreq = new HashMap<>();
        for (MatchedJobResponse j : safe) {
            if (j.missingSkills == null) continue;
            for (String s : j.missingSkills) {
                if (s == null || s.isBlank()) continue;
                missFreq.merge(s.trim().toLowerCase(Locale.ROOT), 1, Integer::sum);
            }
        }
        List<String> topMissing = missFreq.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed())
                .limit(5)
                .map(e -> prettifySkill(e.getKey()))
                .collect(Collectors.toList());
        out.put("topMissingSkills", topMissing);

        out.put("nextRecommendedAction", nextActionJobSeeker(
                missingProfileItems == null ? List.of() : missingProfileItems,
                topMissing,
                safe.size()));

        double avgMatch = safe.isEmpty() ? 0.0
                : safe.stream().mapToDouble(m -> m.matchScore == null ? 0.0 : m.matchScore).average().orElse(0.0);
        double quiz = (attempts == null || attempts.isEmpty() || attempts.get(0).getScore() == null)
                ? 0.0
                : attempts.get(0).getScore();
        out.put("placementReadiness", placementReadinessText(profileCompletionPercent, avgMatch, quiz));

        return out;
    }

    private String nextActionJobSeeker(List<String> missingProfile, List<String> topMissing, int matchCount) {
        if (!missingProfile.isEmpty()) {
            return "Next step: " + missingProfile.get(0) + ". Completing your profile unlocks stronger matches.";
        }
        if (!topMissing.isEmpty()) {
            return "Next step: focus on learning " + String.join(" and ", topMissing.stream().limit(2).toList())
                    + ", then refresh Recommended Jobs.";
        }
        if (matchCount > 0) {
            return "Next step: apply to your top-ranked roles and track status in Applications.";
        }
        return "Next step: add skills and preferred roles so HireNest can surface matching jobs.";
    }

    private String placementReadinessText(int profilePct, double avgMatch, double quiz) {
        double score = profilePct * 0.35 + avgMatch * 0.35 + quiz * 0.30;
        if (score >= 72.0) {
            return "Strong placement readiness â€” your profile depth, match quality, and assessment signal are aligned.";
        }
        if (score >= 52.0) {
            return "Moderate readiness â€” prioritize missing skills and keep your quiz score trending up.";
        }
        return "Early readiness â€” strengthen profile completion, core skills, and assessment before high-volume applications.";
    }

    public Map<String, Object> buildRecruiterInsights(Long recruiterId) {
        Map<Long, List<RankedCandidateDto>> byJob =
                recruiterCandidateService.recruiterCandidates(recruiterId, null, null, null);
        List<Job> jobs = jobService.recruiterJobs(recruiterId);
        Map<Long, String> jobTitleById = jobs.stream()
                .collect(Collectors.toMap(Job::getId, j -> j.getTitle() == null ? "Role" : j.getTitle(), (a, b) -> a));
        List<JobApplicationResponse> apps = recruiterApplicationService.recruiterApplications(recruiterId);

        Map<String, Object> out = new HashMap<>();

        RankedCandidateDto best = null;
        Long bestJobId = null;
        for (Map.Entry<Long, List<RankedCandidateDto>> e : byJob.entrySet()) {
            for (RankedCandidateDto r : e.getValue()) {
                if (best == null) {
                    best = r;
                    bestJobId = e.getKey();
                } else {
                    double rs = r.matchScore == null ? 0.0 : r.matchScore;
                    double bs = best.matchScore == null ? 0.0 : best.matchScore;
                    if (rs > bs) {
                        best = r;
                        bestJobId = e.getKey();
                    }
                }
            }
        }

        Map<String, Object> topCand = new HashMap<>();
        if (best != null && bestJobId != null) {
            topCand.put("candidateName", best.name);
            topCand.put("jobTitle", jobTitleById.getOrDefault(bestJobId, "Job"));
            topCand.put("matchScore", best.matchScore);
            topCand.put("skillsOverlap", best.skillsOverlapCount);
        }
        out.put("topCandidateForJob", topCand);

        Map<String, Integer> missFreq = new HashMap<>();
        for (List<RankedCandidateDto> list : byJob.values()) {
            for (RankedCandidateDto r : list) {
                if (r.missingSkills == null) continue;
                for (String s : r.missingSkills) {
                    if (s == null || s.isBlank()) continue;
                    missFreq.merge(s.trim().toLowerCase(Locale.ROOT), 1, Integer::sum);
                }
            }
        }
        String mostCommon = missFreq.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(e -> prettifySkill(e.getKey()))
                .orElse("");
        out.put("mostCommonMissingSkill", mostCommon.isEmpty() ? null : mostCommon);

        String cluster = "";
        if (best != null && best.skills != null && !best.skills.isBlank()) {
            String[] parts = best.skills.split(",");
            List<String> show = new ArrayList<>();
            for (int i = 0; i < Math.min(4, parts.length); i++) {
                String p = parts[i].trim();
                if (!p.isEmpty()) show.add(prettifySkill(p.toLowerCase(Locale.ROOT)));
            }
            cluster = show.isEmpty() ? "" : String.join(" Â· ", show);
        }
        out.put("strongestSkillCluster", cluster.isEmpty() ? null : cluster);

        long applied = apps.stream().filter(a -> "APPLIED".equalsIgnoreCase(safeStatus(a.status))).count();
        long shortlisted = apps.stream().filter(a -> "SHORTLISTED".equalsIgnoreCase(safeStatus(a.status))).count();
        long rejected = apps.stream().filter(a -> "REJECTED".equalsIgnoreCase(safeStatus(a.status))).count();

        out.put("hiringBottleneck", bottleneckText(applied, shortlisted, rejected, apps.size()));

        return out;
    }

    private static String safeStatus(String s) {
        return s == null ? "" : s.trim();
    }

    private String bottleneckText(long applied, long shortlisted, long rejected, int totalApps) {
        if (totalApps == 0) {
            return "No applications yet â€” promote your postings to build a pipeline.";
        }
        if (applied >= 3 && shortlisted == 0) {
            return "Bottleneck: many candidates remain in Applied with few shortlists â€” review SLAs for first-screening.";
        }
        if (rejected > shortlisted + 2 && rejected > 2) {
            return "High rejection volume vs shortlists â€” revisit job requirements clarity and bar alignment.";
        }
        if (shortlisted > 0 && applied > shortlisted * 3) {
            return "Healthy movement: shortlists exist, but backlog in Applied may need faster triage.";
        }
        return "Pipeline is active â€” monitor time-in-stage per job to spot slowdowns early.";
    }

    private String prettifySkill(String key) {
        if (key == null || key.isBlank()) return "";
        String t = key.trim().toLowerCase(Locale.ROOT);
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
            out.append(Character.toUpperCase(p.charAt(0))).append(p.length() > 1 ? p.substring(1) : "");
        }
        return out.toString();
    }
}

