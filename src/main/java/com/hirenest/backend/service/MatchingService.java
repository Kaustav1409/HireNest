package com.hirenest.backend.service;

import com.hirenest.backend.dto.JobDtos.MatchedJobResponse;
import com.hirenest.backend.dto.JobDtos.LearningRecommendation;
import com.hirenest.backend.dto.JobDtos.MatchingFilterRequest;
import com.hirenest.backend.entity.CandidateProfile;
import com.hirenest.backend.entity.Job;
import com.hirenest.backend.entity.QuizAttempt;
import com.hirenest.backend.entity.RecruiterProfile;
import com.hirenest.backend.repository.JobRepository;
import com.hirenest.backend.repository.QuizAttemptRepository;
import com.hirenest.backend.repository.RecruiterProfileRepository;
import com.hirenest.backend.util.LearningResourceMapper;
import com.hirenest.backend.util.SkillParser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class MatchingService {
    private final JobRepository jobRepository;
    private final ProfileService profileService;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final MatchExplanationService matchExplanationService;
    private final QuizAttemptRepository quizAttemptRepository;

    public MatchingService(JobRepository jobRepository,
                           ProfileService profileService,
                           RecruiterProfileRepository recruiterProfileRepository,
                           MatchExplanationService matchExplanationService,
                           QuizAttemptRepository quizAttemptRepository) {
        this.jobRepository = jobRepository;
        this.profileService = profileService;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.matchExplanationService = matchExplanationService;
        this.quizAttemptRepository = quizAttemptRepository;
    }

    public List<MatchedJobResponse> matching(Long userId) {
        return matching(userId, new MatchingFilterRequest());
    }

    public List<MatchedJobResponse> matching(Long userId, MatchingFilterRequest filter) {
        MatchingFilterRequest f = filter == null ? new MatchingFilterRequest() : filter;
        CandidateProfile profile = profileService.getCandidateOrDefault(userId);
        Set<String> candidateSkills = assessedSkillSet(userId);
        if (candidateSkills.isEmpty()) {
            candidateSkills = candidateSkillSet(profile);
        }
        String preferredRoles = safeLower(profile.getPreferredRoles());
        String preferredLocation = safeLower(profile.getLocation());
        boolean candidateRemotePreferred = Boolean.TRUE.equals(profile.getRemotePreferred());
        List<MatchedJobResponse> output = new ArrayList<>();
        for (Job job : jobRepository.findAll()) {
            if (!matchesFilters(job, f)) {
                continue;
            }
            Set<String> jobSkills = SkillParser.splitSkills(job.getRequiredSkills());
            List<String> matchedSkills = new ArrayList<>();
            List<String> missingSkills = new ArrayList<>();
            double matchScore;
            if (jobSkills.isEmpty()) {
                // Still surface the role: many postings omit requiredSkills; skipping them made Recommended Jobs empty.
                matchScore = 0.0;
            } else {
                for (String s : jobSkills) {
                    if (candidateSkills.contains(s)) {
                        matchedSkills.add(s);
                    } else {
                        missingSkills.add(s);
                    }
                }
                matchScore = (matchedSkills.size() * 100.0) / jobSkills.size();
            }
            Map<String, String> learningLinks = new LinkedHashMap<>();
            List<LearningRecommendation> structuredLearning = new ArrayList<>();
            List<com.hirenest.backend.dto.JobDtos.LearningRoadmapDto> roadmaps = new ArrayList<>();
            for (String skill : missingSkills) {
                roadmaps.add(LearningResourceMapper.roadmapFor(skill));
                LearningRecommendation rec = LearningResourceMapper.recommendationFor(skill);
                structuredLearning.add(rec);
                learningLinks.put(skill, rec.primaryLink);
            }

            MatchedJobResponse row = new MatchedJobResponse();
            row.jobId = job.getId();
            row.title = job.getTitle();
            row.companyName = resolveCompanyName(job);
            row.location = job.getLocation();
            row.remote = job.getRemote();
            row.minSalary = job.getSalaryMin();
            row.maxSalary = job.getSalaryMax();
            row.matchScore = round2(matchScore);
            row.rankingScore = round2(buildRankingScore(
                    row.matchScore,
                    matchedSkills.size(),
                    missingSkills.size(),
                    preferredRoles,
                    preferredLocation,
                    candidateRemotePreferred,
                    job
            ));
            row.matchLabel = toMatchLabel(row.matchScore);
            row.matchedSkills = matchedSkills;
            row.missingSkills = missingSkills;
            row.learningLinks = learningLinks;
            row.learningRecommendations = structuredLearning;
            row.learningRoadmaps = roadmaps;
            row.explanation = matchExplanationService.generateExplanation(round2(matchScore), matchedSkills, missingSkills, job.getTitle());
            row.gapSummary = buildGapSummary(matchedSkills, missingSkills);
            output.add(row);
        }
        output.sort(Comparator
                .comparing((MatchedJobResponse r) -> Optional.ofNullable(r.rankingScore).orElse(0.0)).reversed()
                .thenComparing((MatchedJobResponse r) -> Optional.ofNullable(r.matchScore).orElse(0.0)).reversed()
                .thenComparingInt((MatchedJobResponse r) -> r.matchedSkills == null ? 0 : r.matchedSkills.size()).reversed()
                .thenComparingInt((MatchedJobResponse r) -> r.missingSkills == null ? 0 : r.missingSkills.size())
                .thenComparing((MatchedJobResponse r) -> r.title == null ? "" : r.title));
        return output;
    }

    /** Manual profile skills plus resume-extracted skills (both persisted on {@link CandidateProfile}). */
    private static Set<String> candidateSkillSet(CandidateProfile profile) {
        Set<String> out = new HashSet<>();
        out.addAll(SkillParser.splitSkills(profile.getSkills()));
        out.addAll(SkillParser.splitSkills(profile.getExtractedSkills()));
        return out;
    }

    private Set<String> assessedSkillSet(Long userId) {
        Set<String> out = new HashSet<>();
        if (userId == null) {
            return out;
        }
        List<QuizAttempt> submittedAttempts = quizAttemptRepository
                .findByUserIdAndStatusIgnoreCaseOrderByAttemptedAtDesc(userId, "SUBMITTED");
        for (QuizAttempt attempt : submittedAttempts) {
            String skill = safeLower(attempt.getSkill());
            if (!skill.isBlank()) {
                out.add(skill);
            }
        }
        return out;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String buildGapSummary(List<String> matchedSkills, List<String> missingSkills) {
        int matchedCount = matchedSkills == null ? 0 : matchedSkills.size();
        int missingCount = missingSkills == null ? 0 : missingSkills.size();
        int totalRequired = matchedCount + missingCount;
        if (totalRequired == 0) {
            return "No required skills were defined for this job.";
        }
        if (missingCount == 0) {
            return "You match " + matchedCount + " out of " + totalRequired + " required skills. You currently cover all listed requirements.";
        }
        String focus = formatSkillList(limitList(missingSkills, 3));
        return "You match " + matchedCount + " out of " + totalRequired + " required skills. "
                + "Top missing skills to focus on: " + focus + ". Improving these will increase your eligibility.";
    }

    private List<String> limitList(List<String> in, int n) {
        if (in == null || in.isEmpty()) return List.of();
        if (in.size() <= n) return in;
        return in.subList(0, n);
    }

    private String resolveCompanyName(Job job) {
        if (job == null || job.getRecruiter() == null || job.getRecruiter().getId() == null) {
            return "Unknown company";
        }
        if (job.getCompanyName() != null && !job.getCompanyName().isBlank()) {
            return job.getCompanyName();
        }
        Long recruiterUserId = job.getRecruiter().getId();
        List<RecruiterProfile> rpRows = recruiterProfileRepository.findAllByUserIdOrderByIdAsc(recruiterUserId);
        if (!rpRows.isEmpty()) {
            String cn = rpRows.get(0).getCompanyName();
            if (cn != null && !cn.isBlank()) {
                return cn;
            }
        }
        String fullName = job.getRecruiter().getFullName();
        return (fullName == null || fullName.isBlank()) ? "Unknown company" : fullName;
    }

    private boolean matchesFilters(Job job, MatchingFilterRequest f) {
        if (f == null) return true;

        if (f.remote != null && !Boolean.valueOf(f.remote).equals(Boolean.valueOf(job.getRemote()))) {
            return false;
        }

        if (f.minSalary != null) {
            double jobMax = job.getSalaryMax() == null ? 0.0 : job.getSalaryMax();
            if (jobMax < f.minSalary) return false;
        }
        if (f.maxSalary != null) {
            double jobMin = job.getSalaryMin() == null ? 0.0 : job.getSalaryMin();
            if (jobMin > f.maxSalary) return false;
        }

        if (f.company != null && !f.company.isBlank()) {
            String company = safeLower(resolveCompanyName(job));
            if (!company.contains(safeLower(f.company))) return false;
        }

        if (f.location != null && !f.location.isBlank()) {
            String loc = safeLower(job.getLocation());
            if (!loc.contains(safeLower(f.location))) return false;
        }

        if (f.keyword != null && !f.keyword.isBlank()) {
            String q = safeLower(f.keyword);
            String title = safeLower(job.getTitle());
            String desc = safeLower(job.getDescription());
            String req = safeLower(job.getRequiredSkills());
            if (!title.contains(q) && !desc.contains(q) && !req.contains(q)) return false;
        }
        return true;
    }

    private double buildRankingScore(double matchScore,
                                     int matchedCount,
                                     int missingCount,
                                     String preferredRoles,
                                     String preferredLocation,
                                     boolean candidateRemotePreferred,
                                     Job job) {
        // Clear weighted formula:
        // rankingScore = matchScore + matchedBoost - missingPenalty + roleBoost + locationBoost + remoteBoost
        double matchedBoost = matchedCount * 2.0;
        double missingPenalty = missingCount * 1.5;
        double roleBoost = matchesPreferredRole(job, preferredRoles) ? 6.0 : 0.0;
        double locationBoost = matchesPreferredLocation(job, preferredLocation) ? 4.0 : 0.0;
        double remoteBoost = matchesRemotePreference(job, candidateRemotePreferred) ? 3.0 : 0.0;
        return matchScore + matchedBoost - missingPenalty + roleBoost + locationBoost + remoteBoost;
    }

    private boolean matchesPreferredRole(Job job, String preferredRoles) {
        if (preferredRoles == null || preferredRoles.isBlank()) return false;
        String title = safeLower(job.getTitle());
        String desc = safeLower(job.getDescription());
        for (String role : preferredRoles.split(",")) {
            String r = safeLower(role);
            if (r.isBlank()) continue;
            if (title.contains(r) || desc.contains(r)) return true;
        }
        return false;
    }

    private boolean matchesPreferredLocation(Job job, String preferredLocation) {
        if (preferredLocation == null || preferredLocation.isBlank()) return false;
        String jobLoc = safeLower(job.getLocation());
        return !jobLoc.isBlank() && jobLoc.contains(preferredLocation);
    }

    private boolean matchesRemotePreference(Job job, boolean remotePreferred) {
        if (!remotePreferred) return false;
        return Boolean.TRUE.equals(job.getRemote());
    }

    private String toMatchLabel(Double score) {
        double s = score == null ? 0.0 : score;
        if (s >= 80.0) return "Strong Match";
        if (s >= 60.0) return "Good Match";
        if (s >= 40.0) return "Partial Match";
        return "Low Match";
    }

    private String safeLower(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
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

