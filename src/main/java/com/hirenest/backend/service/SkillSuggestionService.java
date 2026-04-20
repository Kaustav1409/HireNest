package com.hirenest.backend.service;

import com.hirenest.backend.dto.JobDtos.MatchedJobResponse;
import com.hirenest.backend.dto.SkillSuggestionDtos;
import com.hirenest.backend.dto.SkillSuggestionDtos.NamedLink;
import com.hirenest.backend.util.LearningResourceMapper;
import com.hirenest.backend.util.SkillParser;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class SkillSuggestionService {
    private final MatchingService matchingService;
    private final ProfileService profileService;

    public SkillSuggestionService(MatchingService matchingService, ProfileService profileService) {
        this.matchingService = matchingService;
        this.profileService = profileService;
    }

    public List<SkillSuggestionDtos.SkillSuggestion> suggestions(Long userId) {
        // Same skill union as MatchingService (manual + resume-extracted).
        var profile = profileService.getCandidateOrDefault(userId);
        Set<String> candidateSkills = new HashSet<>();
        candidateSkills.addAll(SkillParser.splitSkills(profile.getSkills()));
        candidateSkills.addAll(SkillParser.splitSkills(profile.getExtractedSkills()));

        // Matching service already computes missingSkills per job, sorted by matchScore.
        List<MatchedJobResponse> matchedJobs = matchingService.matching(userId);
        int considered = Math.min(8, matchedJobs.size());
        List<MatchedJobResponse> topJobs = matchedJobs.subList(0, considered);

        Map<String, Integer> missingFreq = new HashMap<>();
        for (MatchedJobResponse jobRow : topJobs) {
            List<String> missing = jobRow.missingSkills;
            if (missing == null) continue;
            for (String s : missing) {
                if (s == null || s.isBlank()) continue;
                // Defensive: only keep skills that are actually missing from profile
                if (candidateSkills.contains(s)) continue;
                missingFreq.merge(s, 1, Integer::sum);
            }
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(missingFreq.entrySet());
        entries.sort(
                Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey)
        );

        int limit = Math.min(12, entries.size());
        List<SkillSuggestionDtos.SkillSuggestion> out = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Integer> e = entries.get(i);
            String skill = e.getKey();
            int freq = e.getValue();

            String importance = mapImportance(freq);
            Map<String, String> learningLinks = new LinkedHashMap<>();
            learningLinks.put("youtube", youtubeSearchLink(skill));
            learningLinks.put("course", courseraSearchLink(skill));
            learningLinks.put("docs", LearningResourceMapper.linkFor(skill));

            Map<String, List<NamedLink>> byLevel = new LinkedHashMap<>();
            byLevel.put("Beginner", List.of(
                    new NamedLink("YouTube â€” basics & tutorials", youtubeSearchLink(skill))
            ));
            byLevel.put("Intermediate", List.of(
                    new NamedLink("Coursera â€” structured courses", courseraSearchLink(skill)),
                    new NamedLink("Practice projects search", googlePracticeLink(skill))
            ));
            byLevel.put("Advanced", List.of(
                    new NamedLink("Official / deep documentation", LearningResourceMapper.linkFor(skill)),
                    new NamedLink("Advanced topics", googleAdvancedLink(skill))
            ));

            SkillSuggestionDtos.SkillSuggestion row = new SkillSuggestionDtos.SkillSuggestion();
            row.skill = skill;
            row.importance = importance;
            row.learningLinks = learningLinks;
            row.linksByLevel = byLevel;
            out.add(row);
        }

        return out;
    }

    private String mapImportance(int freq) {
        // Simple, readable thresholds.
        if (freq >= 3) return "High";
        if (freq == 2) return "Medium";
        return "Low";
    }

    private String youtubeSearchLink(String skill) {
        String q = skill + " tutorial";
        return "https://www.youtube.com/results?search_query=" +
                URLEncoder.encode(q, StandardCharsets.UTF_8);
    }

    private String courseraSearchLink(String skill) {
        String q = skill;
        return "https://www.coursera.org/search?query=" +
                URLEncoder.encode(q, StandardCharsets.UTF_8);
    }

    private String googlePracticeLink(String skill) {
        return "https://www.google.com/search?q=" +
                URLEncoder.encode(skill + " hands-on projects", StandardCharsets.UTF_8);
    }

    private String googleAdvancedLink(String skill) {
        return "https://www.google.com/search?q=" +
                URLEncoder.encode(skill + " advanced best practices", StandardCharsets.UTF_8);
    }
}


