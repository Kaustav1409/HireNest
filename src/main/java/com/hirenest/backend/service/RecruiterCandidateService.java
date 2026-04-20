package com.hirenest.backend.service;

import com.hirenest.backend.dto.RecruiterDtos.RankedCandidateDto;
import com.hirenest.backend.entity.CandidateProfile;
import com.hirenest.backend.entity.Job;
import com.hirenest.backend.entity.QuizAttempt;
import com.hirenest.backend.repository.CandidateProfileRepository;
import com.hirenest.backend.repository.QuizAttemptRepository;
import com.hirenest.backend.util.SkillParser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RecruiterCandidateService {
    private final JobService jobService;
    private final CandidateProfileRepository candidateProfileRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final RecruiterCandidateSummaryService recruiterCandidateSummaryService;

    public RecruiterCandidateService(JobService jobService,
                                     CandidateProfileRepository candidateProfileRepository,
                                     QuizAttemptRepository quizAttemptRepository,
                                     RecruiterCandidateSummaryService recruiterCandidateSummaryService) {
        this.jobService = jobService;
        this.candidateProfileRepository = candidateProfileRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.recruiterCandidateSummaryService = recruiterCandidateSummaryService;
    }

    public Map<Long, List<RankedCandidateDto>> recruiterCandidates(Long recruiterId,
                                                                   String skillFilter,
                                                                   Integer minExperience,
                                                                   Double minMatchScore) {
        String skillNorm = normalizeSkillFilter(skillFilter);
        Integer minExp = minExperience == null ? null : Math.max(0, minExperience);
        Double minScore = minMatchScore == null ? null : minMatchScore;

        Map<Long, List<RankedCandidateDto>> result = new HashMap<>();
        List<CandidateProfile> candidates = candidateProfileRepository.findAll();

        for (Job job : jobService.recruiterJobs(recruiterId)) {
            Set<String> required = SkillParser.splitSkills(job.getRequiredSkills());
            List<RankedCandidateDto> ranked = new ArrayList<>();

            for (CandidateProfile c : candidates) {
                if (c.getUser() == null || c.getUser().getId() == null) {
                    continue;
                }
                Long uid = c.getUser().getId();
                Set<String> has = SkillParser.splitSkills(c.getSkills());

                if (skillNorm != null && !has.contains(skillNorm)) {
                    continue;
                }
                int exp = c.getExperienceYears() == null ? 0 : c.getExperienceYears();
                if (minExp != null && exp < minExp) {
                    continue;
                }

                List<String> matchedSkills = new ArrayList<>();
                List<String> missingSkills = new ArrayList<>();
                for (String s : required) {
                    if (has.contains(s)) {
                        matchedSkills.add(s);
                    } else {
                        missingSkills.add(s);
                    }
                }
                int matched = matchedSkills.size();
                double score = required.isEmpty() ? 0.0 : (matched * 100.0) / required.size();
                if (minScore != null && score < minScore) {
                    continue;
                }

                RankedCandidateDto row = new RankedCandidateDto();
                row.userId = uid;
                row.name = c.getUser().getFullName() == null ? "Unknown" : c.getUser().getFullName();
                row.skills = c.getSkills();
                row.experienceYears = exp;
                row.matchScore = round2(score);
                row.skillsOverlapCount = matched;
                row.jobRequiredSkillCount = required.size();
                row.missingSkills = new ArrayList<>(missingSkills);
                row.latestQuizScorePercent = quizAttemptRepository.findFirstByUserIdOrderByAttemptedAtDesc(uid)
                        .map(QuizAttempt::getScore)
                        .orElse(null);
                boolean hasResume = c.getResumePath() != null && !c.getResumePath().isBlank();
                row.resumeUploaded = hasResume;
                row.resumeDownloadUrl = hasResume ? "/api/profile/candidate/" + uid + "/resume" : null;
                row.candidateSummary = recruiterCandidateSummaryService.buildSummary(
                        job.getTitle(),
                        required.size(),
                        matched,
                        matchedSkills,
                        missingSkills,
                        exp,
                        row.matchScore,
                        row.latestQuizScorePercent,
                        hasResume
                );
                ranked.add(row);
            }

            ranked.sort(Comparator
                    .comparing((RankedCandidateDto r) -> r.matchScore == null ? 0.0 : r.matchScore).reversed()
                    .thenComparing((RankedCandidateDto r) -> r.skillsOverlapCount == null ? 0 : r.skillsOverlapCount).reversed()
                    .thenComparing((RankedCandidateDto r) -> r.name == null ? "" : r.name, String.CASE_INSENSITIVE_ORDER));

            result.put(job.getId(), ranked);
        }
        return result;
    }

    private String normalizeSkillFilter(String skill) {
        if (skill == null || skill.isBlank()) {
            return null;
        }
        return skill.trim().toLowerCase(Locale.ROOT);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

