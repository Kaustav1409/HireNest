package com.hirenest.backend.service;

import com.hirenest.backend.dto.ApplicationDtos;
import com.hirenest.backend.entity.CandidateProfile;
import com.hirenest.backend.entity.Job;
import com.hirenest.backend.entity.QuizAttempt;
import com.hirenest.backend.repository.QuizAttemptRepository;
import com.hirenest.backend.util.SkillParser;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ApplicationStrengthService {

    private final ProfileService profileService;
    private final QuizAttemptRepository quizAttemptRepository;

    public ApplicationStrengthService(ProfileService profileService, QuizAttemptRepository quizAttemptRepository) {
        this.profileService = profileService;
        this.quizAttemptRepository = quizAttemptRepository;
    }

    public ApplicationDtos.ApplicationStrengthPreview analyze(
            Long userId,
            Job job,
            ApplicationDtos.EnhancedApplyRequest request) {
        ApplicationDtos.ApplicationStrengthPreview out = new ApplicationDtos.ApplicationStrengthPreview();
        CandidateProfile profile = profileService.getCandidateOrDefault(userId);
        Set<String> candidateSkills = candidateSkillSet(profile, request);
        Set<String> jobSkills = SkillParser.splitSkills(job.getRequiredSkills());
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String sk : jobSkills) {
            if (candidateSkills.contains(sk)) {
                matched.add(sk);
            } else {
                missing.add(sk);
            }
        }
        double skillMatch = jobSkills.isEmpty() ? 0.0 : matched.size() * 100.0 / jobSkills.size();

        boolean resumePresent = profile.getResumeFileName() != null && !profile.getResumeFileName().isBlank();
        boolean projectPresent = hasProjectEvidence(request);
        boolean assessmentAligned = hasRelevantAssessment(userId, jobSkills);

        double strength = 0.0;
        strength += skillMatch * 0.45;
        if (resumePresent) {
            strength += 15.0;
        }
        if (projectPresent) {
            strength += 20.0;
        }
        if (assessmentAligned) {
            strength += 12.0;
        }
        if (request != null && request.contributionDescription != null && request.contributionDescription.trim().length() > 80) {
            strength += 8.0;
        }
        strength = clamp(strength);

        double confidence = clamp(strength * 0.85 + (assessmentAligned ? 10.0 : 0.0) + (projectPresent ? 5.0 : 0.0));

        out.skillMatchPercent = round2(skillMatch);
        out.resumePresent = resumePresent;
        out.projectPresent = projectPresent;
        out.assessmentAligned = assessmentAligned;
        out.applicationStrengthPercent = round2(strength);
        out.hiringConfidencePercent = round2(confidence);
        out.strengthInsights = buildInsights(matched, missing, resumePresent, projectPresent, assessmentAligned, job.getTitle());
        return out;
    }

    private static Set<String> candidateSkillSet(CandidateProfile profile, ApplicationDtos.EnhancedApplyRequest request) {
        Set<String> skills = new LinkedHashSet<>(SkillParser.splitSkills(profile.getSkills()));
        if (request != null && request.strongestSkills != null) {
            for (String s : request.strongestSkills) {
                if (s != null && !s.isBlank()) {
                    skills.add(s.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return skills;
    }

    private static boolean hasProjectEvidence(ApplicationDtos.EnhancedApplyRequest request) {
        if (request == null) {
            return false;
        }
        return notBlank(request.githubLink)
                || notBlank(request.portfolioLink)
                || notBlank(request.driveLink)
                || (request.projectDescription != null && request.projectDescription.trim().length() > 40);
    }

    private boolean hasRelevantAssessment(Long userId, Set<String> jobSkills) {
        List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdAndStatusIgnoreCaseOrderByAttemptedAtDesc(
                userId, "SUBMITTED");
        for (QuizAttempt a : attempts) {
            if (a.getSkill() == null) {
                continue;
            }
            String skill = a.getSkill().trim().toLowerCase(Locale.ROOT);
            if (jobSkills.contains(skill) && a.getScore() != null && a.getScore() >= 55.0) {
                return true;
            }
        }
        return false;
    }

    private static String buildInsights(
            List<String> matched,
            List<String> missing,
            boolean resume,
            boolean project,
            boolean assessment,
            String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("Application review for ").append(title == null ? "this role" : title).append(": ");
        if (!matched.isEmpty()) {
            sb.append("Strong overlap on ").append(String.join(", ", matched.subList(0, Math.min(4, matched.size()))));
            sb.append(". ");
        }
        if (!missing.isEmpty()) {
            sb.append("Consider highlighting experience in ").append(String.join(", ", missing.subList(0, Math.min(3, missing.size()))));
            sb.append(". ");
        }
        if (!resume) {
            sb.append("Attach an updated resume for a stronger signal. ");
        }
        if (!project) {
            sb.append("Add a GitHub/portfolio or project brief to showcase hands-on work. ");
        }
        if (assessment) {
            sb.append("Relevant skill assessment completed — good credibility signal.");
        } else {
            sb.append("Complete a domain assessment to boost hiring confidence.");
        }
        return sb.toString().trim();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(100.0, v));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
