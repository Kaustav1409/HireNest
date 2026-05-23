/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hirenest.backend.entity.CandidateProfile
 *  com.hirenest.backend.entity.Job
 *  com.hirenest.backend.entity.QuizAttempt
 *  com.hirenest.backend.entity.RecruiterProfile
 *  com.hirenest.backend.repository.QuizAttemptRepository
 *  com.hirenest.backend.repository.RecruiterProfileRepository
 *  com.hirenest.backend.service.MatchExplanationService
 *  com.hirenest.backend.service.ProfileService
 *  com.hirenest.backend.util.LearningResourceMapper
 *  com.hirenest.backend.util.SkillParser
 *  org.springframework.stereotype.Service
 */
package com.hirenest.backend.service;

import com.hirenest.backend.dto.JobDtos;
import com.hirenest.backend.entity.CandidateProfile;
import com.hirenest.backend.entity.Job;
import com.hirenest.backend.entity.QuizAttempt;
import com.hirenest.backend.entity.RecruiterProfile;
import com.hirenest.backend.repository.JobRepository;
import com.hirenest.backend.repository.QuizAttemptRepository;
import com.hirenest.backend.repository.RecruiterProfileRepository;
import com.hirenest.backend.service.MatchExplanationService;
import com.hirenest.backend.service.ProfileService;
import com.hirenest.backend.util.LearningResourceMapper;
import com.hirenest.backend.util.SkillParser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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

    public MatchingService(JobRepository jobRepository, ProfileService profileService, RecruiterProfileRepository recruiterProfileRepository, MatchExplanationService matchExplanationService, QuizAttemptRepository quizAttemptRepository) {
        this.jobRepository = jobRepository;
        this.profileService = profileService;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.matchExplanationService = matchExplanationService;
        this.quizAttemptRepository = quizAttemptRepository;
    }

    public List<JobDtos.MatchedJobResponse> matching(Long userId) {
        return this.matching(userId, new JobDtos.MatchingFilterRequest());
    }

    public List<JobDtos.MatchedJobResponse> matching(Long userId, JobDtos.MatchingFilterRequest filter) {
        JobDtos.MatchingFilterRequest f = filter == null ? new JobDtos.MatchingFilterRequest() : filter;
        CandidateProfile profile = this.profileService.getCandidateOrDefault(userId);
        Set<String> candidateSkills = this.assessedSkillSet(userId);
        if (candidateSkills.isEmpty()) {
            candidateSkills = MatchingService.candidateSkillSet(profile);
        }
        String preferredRoles = this.safeLower(profile.getPreferredRoles());
        String preferredLocation = this.safeLower(profile.getLocation());
        boolean candidateRemotePreferred = Boolean.TRUE.equals(profile.getRemotePreferred());
        ArrayList<JobDtos.MatchedJobResponse> output = new ArrayList<JobDtos.MatchedJobResponse>();
        for (Job job : this.jobRepository.findAll()) {
            double matchScore;
            if (!this.matchesFilters(job, f)) continue;
            Set<String> jobSkills = SkillParser.splitSkills(job.getRequiredSkills());
            ArrayList<String> matchedSkills = new ArrayList<String>();
            ArrayList<String> missingSkills = new ArrayList<String>();
            if (jobSkills.isEmpty()) {
                matchScore = 0.0;
            } else {
                for (String s : jobSkills) {
                    if (candidateSkills.contains(s)) {
                        matchedSkills.add(s);
                        continue;
                    }
                    missingSkills.add(s);
                }
                matchScore = (double)matchedSkills.size() * 100.0 / (double)jobSkills.size();
            }
            LinkedHashMap<String, String> learningLinks = new LinkedHashMap<String, String>();
            ArrayList<JobDtos.LearningRecommendation> structuredLearning = new ArrayList<JobDtos.LearningRecommendation>();
            ArrayList<JobDtos.LearningRoadmapDto> roadmaps = new ArrayList<JobDtos.LearningRoadmapDto>();
            for (String skill : missingSkills) {
                roadmaps.add(LearningResourceMapper.roadmapFor((String)skill));
                JobDtos.LearningRecommendation rec = LearningResourceMapper.recommendationFor((String)skill);
                structuredLearning.add(rec);
                learningLinks.put(skill, rec.primaryLink);
            }
            JobDtos.MatchedJobResponse row = new JobDtos.MatchedJobResponse();
            row.jobId = job.getId();
            row.title = job.getTitle();
            row.companyName = this.resolveCompanyName(job);
            row.location = job.getLocation();
            row.remote = job.getRemote();
            row.minSalary = job.getSalaryMin();
            row.maxSalary = job.getSalaryMax();
            row.matchScore = this.round2(matchScore);
            row.rankingScore = this.round2(this.buildRankingScore(row.matchScore, matchedSkills.size(), missingSkills.size(), preferredRoles, preferredLocation, candidateRemotePreferred, job));
            row.matchLabel = this.toMatchLabel(row.matchScore);
            row.matchedSkills = matchedSkills;
            row.missingSkills = missingSkills;
            row.learningLinks = learningLinks;
            row.learningRecommendations = structuredLearning;
            row.learningRoadmaps = roadmaps;
            row.explanation = this.matchExplanationService.generateExplanation(this.round2(matchScore), matchedSkills, missingSkills, job.getTitle());
            row.gapSummary = this.buildGapSummary(matchedSkills, missingSkills);
            row.platformJob = job.getPlatformJob();
            row.postedByLabel = job.getPostedByLabel();
            row.experienceLevel = job.getExperienceLevel();
            row.category = job.getCategory();
            row.roleType = job.getRoleType();
            output.add(row);
        }
        output.sort(
                Comparator.comparing((JobDtos.MatchedJobResponse r) -> Optional.ofNullable(r.rankingScore).orElse(0.0))
                        .reversed()
                        .thenComparing((JobDtos.MatchedJobResponse r) -> Optional.ofNullable(r.matchScore).orElse(0.0))
                        .reversed()
                        .thenComparingInt((JobDtos.MatchedJobResponse r) -> r.matchedSkills == null ? 0 : r.matchedSkills.size())
                        .reversed()
                        .thenComparingInt((JobDtos.MatchedJobResponse r) -> r.missingSkills == null ? 0 : r.missingSkills.size())
                        .thenComparing((JobDtos.MatchedJobResponse r) -> r.title == null ? "" : r.title));
        return output;
    }

    private static Set<String> candidateSkillSet(CandidateProfile profile) {
        HashSet<String> out = new HashSet<String>();
        out.addAll(SkillParser.splitSkills((String)profile.getSkills()));
        out.addAll(SkillParser.splitSkills((String)profile.getExtractedSkills()));
        return out;
    }

    private Set<String> assessedSkillSet(Long userId) {
        HashSet<String> out = new HashSet<String>();
        if (userId == null) {
            return out;
        }
        List<QuizAttempt> submittedAttempts =
                this.quizAttemptRepository.findByUserIdAndStatusIgnoreCaseOrderByAttemptedAtDesc(userId, "SUBMITTED");
        for (QuizAttempt attempt : submittedAttempts) {
            String skill = this.safeLower(attempt.getSkill());
            if (skill.isBlank()) continue;
            out.add(skill);
        }
        return out;
    }

    private double round2(double value) {
        return (double)Math.round(value * 100.0) / 100.0;
    }

    private String buildGapSummary(List<String> matchedSkills, List<String> missingSkills) {
        int missingCount;
        int matchedCount = matchedSkills == null ? 0 : matchedSkills.size();
        int totalRequired = matchedCount + (missingCount = missingSkills == null ? 0 : missingSkills.size());
        if (totalRequired == 0) {
            return "No required skills were defined for this job.";
        }
        if (missingCount == 0) {
            return "You match " + matchedCount + " out of " + totalRequired + " required skills. You currently cover all listed requirements.";
        }
        String focus = MatchingService.formatSkillList(this.limitList(missingSkills, 3));
        return "You match " + matchedCount + " out of " + totalRequired + " required skills. Top missing skills to focus on: " + focus + ". Improving these will increase your eligibility.";
    }

    private List<String> limitList(List<String> in, int n) {
        if (in == null || in.isEmpty()) {
            return List.of();
        }
        if (in.size() <= n) {
            return in;
        }
        return in.subList(0, n);
    }

    private String resolveCompanyName(Job job) {
        String cn;
        if (job == null || job.getRecruiter() == null || job.getRecruiter().getId() == null) {
            return "Unknown company";
        }
        if (job.getCompanyName() != null && !job.getCompanyName().isBlank()) {
            return job.getCompanyName();
        }
        Long recruiterUserId = job.getRecruiter().getId();
        List rpRows = this.recruiterProfileRepository.findAllByUserIdOrderByIdAsc(recruiterUserId);
        if (!rpRows.isEmpty() && (cn = ((RecruiterProfile)rpRows.get(0)).getCompanyName()) != null && !cn.isBlank()) {
            return cn;
        }
        String fullName = job.getRecruiter().getFullName();
        return fullName == null || fullName.isBlank() ? "Unknown company" : fullName;
    }

    private boolean matchesFilters(Job job, JobDtos.MatchingFilterRequest f) {
        String loc;
        String company;
        if (f == null) {
            return true;
        }
        if (f.remote != null && !Boolean.valueOf(f.remote).equals((boolean)job.getRemote())) {
            return false;
        }
        if (f.minSalary != null) {
            double jobMax;
            double d = jobMax = job.getSalaryMax() == null ? 0.0 : job.getSalaryMax();
            if (jobMax < f.minSalary) {
                return false;
            }
        }
        if (f.maxSalary != null) {
            double jobMin;
            double d = jobMin = job.getSalaryMin() == null ? 0.0 : job.getSalaryMin();
            if (jobMin > f.maxSalary) {
                return false;
            }
        }
        if (f.company != null && !f.company.isBlank() && !(company = this.safeLower(this.resolveCompanyName(job))).contains(this.safeLower(f.company))) {
            return false;
        }
        if (f.location != null && !f.location.isBlank() && !(loc = this.safeLower(job.getLocation())).contains(this.safeLower(f.location))) {
            return false;
        }
        if (f.keyword != null && !f.keyword.isBlank()) {
            String q = this.safeLower(f.keyword);
            String title = this.safeLower(job.getTitle());
            String desc = this.safeLower(job.getDescription());
            String req = this.safeLower(job.getRequiredSkills());
            if (!(title.contains(q) || desc.contains(q) || req.contains(q))) {
                return false;
            }
        }
        return true;
    }

    private double buildRankingScore(double matchScore, int matchedCount, int missingCount, String preferredRoles, String preferredLocation, boolean candidateRemotePreferred, Job job) {
        double matchedBoost = (double)matchedCount * 2.0;
        double missingPenalty = (double)missingCount * 1.5;
        double roleBoost = this.matchesPreferredRole(job, preferredRoles) ? 6.0 : 0.0;
        double locationBoost = this.matchesPreferredLocation(job, preferredLocation) ? 4.0 : 0.0;
        double remoteBoost = this.matchesRemotePreference(job, candidateRemotePreferred) ? 3.0 : 0.0;
        return matchScore + matchedBoost - missingPenalty + roleBoost + locationBoost + remoteBoost;
    }

    private boolean matchesPreferredRole(Job job, String preferredRoles) {
        if (preferredRoles == null || preferredRoles.isBlank()) {
            return false;
        }
        String title = this.safeLower(job.getTitle());
        String desc = this.safeLower(job.getDescription());
        for (String role : preferredRoles.split(",")) {
            String r = this.safeLower(role);
            if (r.isBlank() || !title.contains(r) && !desc.contains(r)) continue;
            return true;
        }
        return false;
    }

    private boolean matchesPreferredLocation(Job job, String preferredLocation) {
        if (preferredLocation == null || preferredLocation.isBlank()) {
            return false;
        }
        String jobLoc = this.safeLower(job.getLocation());
        return !jobLoc.isBlank() && jobLoc.contains(preferredLocation);
    }

    private boolean matchesRemotePreference(Job job, boolean remotePreferred) {
        if (!remotePreferred) {
            return false;
        }
        return Boolean.TRUE.equals(job.getRemote());
    }

    private String toMatchLabel(Double score) {
        double s;
        double d = s = score == null ? 0.0 : score;
        if (s >= 80.0) {
            return "Strong Match";
        }
        if (s >= 60.0) {
            return "Good Match";
        }
        if (s >= 40.0) {
            return "Partial Match";
        }
        return "Low Match";
    }

    private String safeLower(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

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
        for (int i = 0; i < skills.size(); ++i) {
            if (i > 0) {
                sb.append(i == skills.size() - 1 ? ", and " : ", ");
            }
            sb.append(skills.get(i));
        }
        return sb.toString();
    }
}
