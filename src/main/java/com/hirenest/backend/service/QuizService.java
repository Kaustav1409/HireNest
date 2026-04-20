package com.hirenest.backend.service;

import com.hirenest.backend.dto.QuizDtos.Answer;
import com.hirenest.backend.dto.QuizDtos.QuizQuestionResponse;
import com.hirenest.backend.dto.QuizDtos.QuizRecommendedAssessment;
import com.hirenest.backend.dto.QuizDtos.QuizResultResponse;
import com.hirenest.backend.dto.QuizDtos.QuizSubmission;
import com.hirenest.backend.dto.QuizDtos.QuizAttemptStartRequest;
import com.hirenest.backend.dto.QuizDtos.QuizAttemptStartResponse;
import com.hirenest.backend.dto.QuizDtos.QuizAttemptHeartbeatRequest;
import com.hirenest.backend.dto.QuizDtos.QuizAttemptViolationRequest;
import com.hirenest.backend.dto.QuizDtos.AssessmentOverviewItem;
import com.hirenest.backend.dto.QuizDtos.UserSkillProfileItem;
import com.hirenest.backend.entity.CandidateProfile;
import com.hirenest.backend.entity.QuizAttempt;
import com.hirenest.backend.entity.QuizQuestion;
import com.hirenest.backend.entity.User;
import com.hirenest.backend.exception.BadRequestException;
import com.hirenest.backend.exception.NotFoundException;
import com.hirenest.backend.repository.QuizAttemptRepository;
import com.hirenest.backend.repository.QuizQuestionRepository;
import com.hirenest.backend.repository.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class QuizService {
    /** Fixed assessment size target. */
    private static final int TARGET_QUIZ_SIZE = 25;
    /** Keep same as target so one full assessment is returned. */
    private static final int MAX_QUIZ_QUESTIONS_PER_LOAD = TARGET_QUIZ_SIZE;
    private static final int BEGINNER_TARGET = 6;
    private static final int INTERMEDIATE_TARGET = 9;
    private static final int ADVANCED_TARGET = 10;
    private static final int HEARTBEAT_INTERVAL_SECONDS = 5;
    private static final int HEARTBEAT_TIMEOUT_SECONDS = 20;
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_INVALIDATED = "INVALIDATED";

    private static final List<String> CANONICAL_SKILLS = List.of(
            "java",
            "python",
            "javascript",
            "react",
            "spring boot",
            "sql",
            "mongodb",
            "rest apis",
            "git",
            "cloud basics",
            "dbms",
            "data structures",
            "python for data",
            "sql analytics",
            "power bi",
            "tableau",
            "statistics",
            "data cleaning",
            "machine learning basics",
            "aptitude",
            "html",
            "css",
            "ui principles",
            "ux principles",
            "wireframing",
            "prototyping",
            "user research",
            "design thinking",
            "figma",
            "adobe xd",
            "typography",
            "color theory",
            "photoshop",
            "illustrator",
            "canva",
            "branding",
            "layout design",
            "poster design",
            "social media design",
            "visual hierarchy",
            "video editing",
            "premiere pro",
            "capcut",
            "after effects basics",
            "color grading",
            "storytelling",
            "thumbnail design",
            "script writing",
            "seo",
            "excel",
            "social media marketing",
            "content marketing",
            "email marketing",
            "marketing analytics",
            "branding strategy",
            "network security",
            "ethical hacking basics",
            "risk analysis",
            "security fundamentals",
            "communication",
            "problem solving",
            "time management",
            "teamwork",
            "autocad",
            "circuit design",
            "mechanical basics",
            "civil basics"
    );

    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final UserRepository userRepository;
    private final ProfileService profileService;
    private final QuizLearningIntegrationService quizLearningIntegrationService;
    private final QuizAssessmentRecommendationService quizAssessmentRecommendationService;

    public QuizService(QuizQuestionRepository quizQuestionRepository,
                       QuizAttemptRepository quizAttemptRepository,
                       UserRepository userRepository,
                       ProfileService profileService,
                       QuizLearningIntegrationService quizLearningIntegrationService,
                       QuizAssessmentRecommendationService quizAssessmentRecommendationService) {
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.userRepository = userRepository;
        this.profileService = profileService;
        this.quizLearningIntegrationService = quizLearningIntegrationService;
        this.quizAssessmentRecommendationService = quizAssessmentRecommendationService;
    }

    /** Public catalog for UI dropdowns (slug values match {@link #normalizeSkill(String)}). */
    public List<String> listDomains() {
        return new ArrayList<>(CANONICAL_SKILLS);
    }

    /**
     * Suggested quiz domains based on merged profile skills and resume-extracted skills.
     */
    public List<QuizRecommendedAssessment> recommendedAssessments(Long userId) {
        if (userId == null) {
            throw new BadRequestException("userId is required");
        }
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found");
        }
        CandidateProfile profile = profileService.getCandidateOrDefault(userId);
        return quizAssessmentRecommendationService.recommend(profile, CANONICAL_SKILLS);
    }

    public List<AssessmentOverviewItem> assessmentOverview(Long userId) {
        if (userId == null) {
            throw new BadRequestException("userId is required");
        }
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found");
        }
        ensureCatalog();
        List<AssessmentOverviewItem> out = new ArrayList<>();
        for (String slug : CANONICAL_SKILLS) {
            String category = categoryForSkill(slug);
            String defaultDifficulty = defaultDifficultyForSkill(slug);
            int questionCount = (int) quizQuestionRepository.countBySkillIgnoreCase(slug);
            int estimatedMinutes = Math.max(8, (int) Math.ceil(questionCount * 0.75));
            QuizAttempt latest = quizAttemptRepository
                    .findTopByUserIdAndSkillIgnoreCaseOrderByAttemptedAtDesc(userId, slug)
                    .orElse(null);
            String status = "NOT_STARTED";
            Double lastScore = null;
            if (latest != null) {
                if (STATUS_IN_PROGRESS.equalsIgnoreCase(latest.getStatus())) {
                    status = "IN_PROGRESS";
                } else if (STATUS_SUBMITTED.equalsIgnoreCase(latest.getStatus())) {
                    status = "COMPLETED";
                    lastScore = latest.getScore();
                }
            }
            out.add(new AssessmentOverviewItem(
                    slug,
                    formatSkillLabel(slug),
                    category,
                    defaultDifficulty,
                    questionCount,
                    estimatedMinutes,
                    status,
                    lastScore
            ));
        }
        return out;
    }

    public List<UserSkillProfileItem> userSkillProfile(Long userId) {
        if (userId == null) {
            throw new BadRequestException("userId is required");
        }
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found");
        }
        List<QuizAttempt> submitted = quizAttemptRepository
                .findByUserIdAndStatusIgnoreCaseOrderByAttemptedAtDesc(userId, STATUS_SUBMITTED);
        List<UserSkillProfileItem> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (QuizAttempt attempt : submitted) {
            String skill = normalizeSkill(attempt.getSkill());
            if (skill.isBlank() || seen.contains(skill)) {
                continue;
            }
            seen.add(skill);
            double score = attempt.getScore() == null ? 0.0 : attempt.getScore();
            out.add(new UserSkillProfileItem(
                    formatSkillLabel(skill),
                    round2(score),
                    skillLevel(score),
                    "COMPLETED"
            ));
        }
        return out;
    }

    /**
     * Structured dataset preview for requested skills.
     * Each skill is returned with 25 unique questions and explicit answer letter.
     */
    public java.util.Map<String, List<java.util.Map<String, Object>>> sampleQuestionDataset(List<String> skills) {
        ensureCatalog();
        List<String> requested = (skills == null || skills.isEmpty())
                ? List.of("java", "python", "figma")
                : skills;
        java.util.Map<String, List<java.util.Map<String, Object>>> out = new LinkedHashMap<>();
        for (String raw : requested) {
            String sk = normalizeSkill(raw);
            if (!CANONICAL_SKILLS.contains(sk)) {
                throw new BadRequestException("Unsupported skill: " + raw);
            }
            List<QuizQuestion> rows = new ArrayList<>(quizQuestionRepository.findBySkillIgnoreCase(sk));
            if (!isValidSkillSet(rows)) {
                throw new BadRequestException("Question set is invalid for skill: " + sk);
            }
            rows.sort(Comparator
                    .comparingInt((QuizQuestion q) -> difficultyRank(q.getDifficulty()))
                    .thenComparing(QuizQuestion::getQuestion, String.CASE_INSENSITIVE_ORDER));
            List<java.util.Map<String, Object>> items = new ArrayList<>();
            for (QuizQuestion q : rows) {
                java.util.Map<String, Object> item = new LinkedHashMap<>();
                item.put("question", q.getQuestion());
                item.put("options", optionsAsList(q.getOptionsCsv()));
                item.put("correctAnswer", answerLetter(q.getCorrectIndex()));
                item.put("difficulty", q.getDifficulty() == null ? "" : q.getDifficulty().trim().toUpperCase(Locale.ROOT));
                items.add(item);
            }
            out.put(formatSkillLabel(sk), items);
        }
        return out;
    }

    public QuizAttemptStartResponse startAttempt(QuizAttemptStartRequest request) {
        if (request == null || request.userId == null) {
            throw new BadRequestException("userId is required");
        }
        Long userId = request.userId;
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        String skillNorm = request.skill == null ? "general" : normalizeSkill(request.skill);
        String diffNorm = request.difficulty == null || request.difficulty.isBlank() ? null : request.difficulty.trim();
        LocalDateTime now = LocalDateTime.now();
        QuizAttempt a = new QuizAttempt();
        a.setUser(user);
        a.setSkill(skillNorm);
        a.setDifficulty(diffNorm);
        a.setStatus(STATUS_IN_PROGRESS);
        a.setStartedAt(now);
        a.setLastHeartbeatAt(now);
        a.setAttemptedAt(now);
        quizAttemptRepository.save(a);

        QuizAttemptStartResponse out = new QuizAttemptStartResponse();
        out.attemptId = a.getId();
        out.heartbeatIntervalSeconds = HEARTBEAT_INTERVAL_SECONDS;
        out.heartbeatTimeoutSeconds = HEARTBEAT_TIMEOUT_SECONDS;
        out.status = STATUS_IN_PROGRESS;
        return out;
    }

    public void heartbeat(QuizAttemptHeartbeatRequest request) {
        QuizAttempt a = resolveAttempt(request == null ? null : request.userId, request == null ? null : request.attemptId);
        if (!STATUS_IN_PROGRESS.equalsIgnoreCase(a.getStatus())) {
            throw new BadRequestException("Attempt is not active");
        }
        a.setLastHeartbeatAt(LocalDateTime.now());
        quizAttemptRepository.save(a);
    }

    public void reportViolation(QuizAttemptViolationRequest request) {
        QuizAttempt a = resolveAttempt(request == null ? null : request.userId, request == null ? null : request.attemptId);
        invalidateAttempt(a, request == null ? null : request.reason);
    }

    /**
     * Loads questions for a domain.
     * <ul>
     *   <li>If difficulty is provided, uses only that tier (up to {@link #TARGET_QUIZ_SIZE}).</li>
     *   <li>If not provided, builds a mixed assessment with targets:
     *       6 Beginner, 9 Intermediate, 10 Advanced, then fills remaining slots to 25 from any leftover rows.</li>
     * </ul>
     */
    public List<QuizQuestionResponse> quiz(String skill, String difficulty) {
        ensureCatalog();
        String sk = normalizeSkill(skill);
        if (sk.isEmpty()) {
            throw new BadRequestException("skill is required");
        }
        List<QuizQuestion> pool = buildQuestionPool(sk, difficulty);
        Collections.shuffle(pool, ThreadLocalRandom.current());
        int take = Math.min(pool.size(), MAX_QUIZ_QUESTIONS_PER_LOAD);
        if (take < pool.size()) {
            pool = pool.subList(0, take);
        }
        List<QuizQuestionResponse> out = new ArrayList<>();
        for (QuizQuestion q : pool) {
            QuizQuestionResponse r = new QuizQuestionResponse();
            r.quizId = q.getId();
            r.skill = q.getSkill();
            r.difficulty = q.getDifficulty();
            r.question = q.getQuestion();
            r.options = List.of(q.getOptionsCsv().split("\\|"));
            out.add(r);
        }
        return out;
    }

    private List<QuizQuestion> buildQuestionPool(String sk, String difficulty) {
        boolean hasDiff = difficulty != null && !difficulty.isBlank();

        if (hasDiff) {
            String diff = difficulty == null ? "" : difficulty.trim();
            List<QuizQuestion> byDiff = new ArrayList<>(quizQuestionRepository.findBySkillIgnoreCaseAndDifficultyIgnoreCase(sk, diff));
            Collections.shuffle(byDiff, ThreadLocalRandom.current());
            if (byDiff.size() > TARGET_QUIZ_SIZE) {
                return new ArrayList<>(byDiff.subList(0, TARGET_QUIZ_SIZE));
            }
            return byDiff;
        }

        List<QuizQuestion> beginner = new ArrayList<>(quizQuestionRepository.findBySkillIgnoreCaseAndDifficultyIgnoreCase(sk, "Beginner"));
        List<QuizQuestion> intermediate = new ArrayList<>(quizQuestionRepository.findBySkillIgnoreCaseAndDifficultyIgnoreCase(sk, "Intermediate"));
        List<QuizQuestion> advanced = new ArrayList<>(quizQuestionRepository.findBySkillIgnoreCaseAndDifficultyIgnoreCase(sk, "Advanced"));
        Collections.shuffle(beginner, ThreadLocalRandom.current());
        Collections.shuffle(intermediate, ThreadLocalRandom.current());
        Collections.shuffle(advanced, ThreadLocalRandom.current());

        List<QuizQuestion> selected = new ArrayList<>();
        addUpTo(selected, beginner, BEGINNER_TARGET);
        addUpTo(selected, intermediate, INTERMEDIATE_TARGET);
        addUpTo(selected, advanced, ADVANCED_TARGET);

        if (selected.size() >= TARGET_QUIZ_SIZE) {
            return selected.subList(0, TARGET_QUIZ_SIZE);
        }

        Set<Long> seen = selected.stream().map(QuizQuestion::getId).collect(Collectors.toCollection(HashSet::new));
        List<QuizQuestion> rest = quizQuestionRepository.findBySkillIgnoreCase(sk).stream()
                .filter(q -> q.getId() != null && !seen.contains(q.getId()))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(rest, ThreadLocalRandom.current());
        for (QuizQuestion q : rest) {
            if (selected.size() >= TARGET_QUIZ_SIZE) {
                break;
            }
            selected.add(q);
        }
        return selected;
    }

    private static void addUpTo(List<QuizQuestion> out, List<QuizQuestion> source, int target) {
        for (int i = 0; i < source.size() && i < target; i++) {
            out.add(source.get(i));
        }
    }

    public QuizResultResponse submitQuiz(QuizSubmission submission) {
        if (submission == null) {
            throw new BadRequestException("submission is required");
        }
        QuizAttempt activeAttempt = resolveAttempt(submission.userId, submission.attemptId);
        validateAttemptForSubmit(activeAttempt);
        int total = submission.answers == null ? 0 : submission.answers.size();
        int correct = 0;
        if (submission.answers != null) {
            for (Answer a : submission.answers) {
                if (a == null || a.quizId == null) {
                    continue;
                }
                QuizQuestion q = quizQuestionRepository.findById(a.quizId).orElse(null);
                if (q != null && q.getCorrectIndex().equals(a.selectedIndex)) {
                    correct++;
                }
            }
        }
        double score = total == 0 ? 0.0 : (correct * 100.0 / total);
        Long userId = submission.userId;
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        String skillNorm = submission.skill == null ? "general" : normalizeSkill(submission.skill);
        String diffNorm = submission.difficulty == null || submission.difficulty.isBlank()
                ? null
                : submission.difficulty.trim();
        activeAttempt.setUser(user);
        activeAttempt.setSkill(skillNorm);
        activeAttempt.setDifficulty(diffNorm);
        activeAttempt.setScore(round2(score));
        activeAttempt.setCorrectAnswers(correct);
        activeAttempt.setTotalQuestions(total);
        activeAttempt.setDurationSeconds(submission.durationSeconds == null ? 0 : submission.durationSeconds);
        activeAttempt.setStatus(STATUS_SUBMITTED);
        activeAttempt.setAttemptedAt(LocalDateTime.now());
        quizAttemptRepository.save(activeAttempt);

        String performance = skillLevel(score);
        QuizResultResponse result = new QuizResultResponse();
        result.score = round2(score);
        result.totalQuestions = total;
        result.correctAnswers = correct;
        result.durationSeconds = activeAttempt.getDurationSeconds();
        result.skill = skillNorm;
        result.difficulty = diffNorm;
        result.skillLevel = performance;
        result.performanceLevel = performance;
        result.feedback = buildFeedback(skillNorm, score, total);
        quizLearningIntegrationService.attachLearningFollowUp(result);
        return result;
    }

    private QuizAttempt resolveAttempt(Long userId, Long attemptId) {
        if (userId == null) {
            throw new BadRequestException("userId is required");
        }
        if (attemptId == null) {
            throw new BadRequestException("attemptId is required");
        }
        return quizAttemptRepository.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new NotFoundException("Quiz attempt not found"));
    }

    private void validateAttemptForSubmit(QuizAttempt attempt) {
        if (!STATUS_IN_PROGRESS.equalsIgnoreCase(attempt.getStatus())) {
            throw new BadRequestException("Attempt is not active");
        }
        LocalDateTime hb = attempt.getLastHeartbeatAt();
        if (hb == null || hb.isBefore(LocalDateTime.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS))) {
            invalidateAttempt(attempt, "HEARTBEAT_TIMEOUT");
            throw new BadRequestException("Attempt invalidated due to heartbeat timeout");
        }
    }

    private void invalidateAttempt(QuizAttempt attempt, String reason) {
        attempt.setStatus(STATUS_INVALIDATED);
        attempt.setInvalidatedAt(LocalDateTime.now());
        String normalizedReason = (reason == null || reason.isBlank()) ? "VIOLATION" : reason.trim();
        attempt.setInvalidationReason(normalizedReason);
        quizAttemptRepository.save(attempt);
    }

    /**
     * Performance tier from score: Advanced â‰¥80%, Intermediate â‰¥50%, else Beginner.
     */
    private String skillLevel(double score) {
        if (score >= 80.0) {
            return "Advanced";
        }
        if (score >= 50.0) {
            return "Intermediate";
        }
        return "Beginner";
    }

    /**
     * Human-readable feedback: combines tier-based encouragement with optional domain-specific tips.
     */
    private String buildFeedback(String skillSlug, double score, int totalQuestions) {
        if (totalQuestions <= 0) {
            return "No answers were submitted.";
        }
        String label = formatSkillLabel(skillSlug);
        StringBuilder sb = new StringBuilder();
        if (score >= 80.0) {
            sb.append("You have a strong understanding of ").append(label).append(" fundamentals.");
        } else if (score >= 50.0) {
            sb.append("You have a solid foundation in ").append(label)
                    .append(". Keep practicing harder topics to reach an advanced level.");
        } else {
            sb.append("Focus on core concepts in ").append(label)
                    .append(". Review the basics and try the assessment again when ready.");
        }
        String extra = domainImprovementHint(skillSlug, score);
        if (extra != null && !extra.isBlank()) {
            sb.append(" ").append(extra);
        }
        return sb.toString().trim();
    }

    /**
     * Optional second sentence for specific domains when the score suggests gaps (below 80%).
     */
    private String domainImprovementHint(String skillSlug, double score) {
        if (score >= 80.0) {
            return "";
        }
        return switch (skillSlug) {
            case "sql", "dbms" -> "You should improve SQL joins and normalization concepts.";
            case "java" -> "You could strengthen Java basics, OOP, and collections.";
            case "python" -> "Practice functions, data structures, and idiomatic Python patterns.";
            case "javascript" -> "Review closures, async flows, and strict equality.";
            case "react" -> "Revisit hooks, state lifting, and component lifecycle patterns.";
            case "spring boot" -> "Study configuration, REST controllers, and dependency injection.";
            case "data structures" -> "Work through trees, graphs, and complexity analysis.";
            case "aptitude" -> "Practice ratios, percentages, and logical reasoning drills.";
            case "web development" -> "Review HTTP, security basics (CORS, CSP), and REST design.";
            default -> "";
        };
    }

    private String formatSkillLabel(String normalizedSlug) {
        if (normalizedSlug == null || normalizedSlug.isBlank() || "general".equals(normalizedSlug)) {
            return "this skill area";
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

    private String categoryForSkill(String slug) {
        return switch (slug) {
            case "java", "python", "javascript", "react", "spring boot", "sql", "mongodb", "rest apis",
                    "git", "cloud basics", "html", "css", "dbms", "data structures" -> "Software / IT";
            case "python for data", "sql analytics", "excel", "power bi", "tableau", "statistics",
                    "data cleaning", "machine learning basics" -> "Data / AI";
            case "ui principles", "ux principles", "wireframing", "prototyping", "user research",
                    "design thinking", "figma", "adobe xd", "typography", "color theory" -> "UI/UX";
            case "photoshop", "illustrator", "canva", "branding", "layout design", "poster design",
                    "social media design", "visual hierarchy" -> "Graphic Design";
            case "video editing", "premiere pro", "capcut", "after effects basics", "color grading",
                    "storytelling", "thumbnail design", "script writing" -> "Video / Creative";
            case "seo", "social media marketing", "content marketing", "email marketing", "marketing analytics",
                    "branding strategy" -> "Marketing";
            case "network security", "ethical hacking basics", "risk analysis", "security fundamentals" -> "Cybersecurity";
            case "autocad", "circuit design", "mechanical basics", "civil basics" -> "Core Engineering";
            case "aptitude", "communication", "problem solving", "time management", "teamwork" -> "General Employability";
            default -> "Software / IT";
        };
    }

    private String defaultDifficultyForSkill(String slug) {
        return switch (slug) {
            case "java", "python", "javascript", "react", "spring boot", "sql", "data structures",
                    "mongodb", "rest apis", "git", "power bi", "tableau", "network security" -> "Intermediate";
            default -> "Beginner";
        };
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private int difficultyRank(String difficulty) {
        String d = difficulty == null ? "" : difficulty.trim().toUpperCase(Locale.ROOT);
        if ("BEGINNER".equals(d)) return 0;
        if ("INTERMEDIATE".equals(d)) return 1;
        if ("ADVANCED".equals(d)) return 2;
        return 99;
    }

    private List<String> optionsAsList(String optionsCsv) {
        if (optionsCsv == null || optionsCsv.isBlank()) {
            return List.of();
        }
        String[] arr = optionsCsv.split("\\|");
        List<String> out = new ArrayList<>();
        for (String s : arr) {
            out.add(s == null ? "" : s.trim());
        }
        return out;
    }

    private String answerLetter(Integer idx) {
        if (idx == null || idx < 0 || idx > 3) {
            return "";
        }
        return switch (idx) {
            case 0 -> "A";
            case 1 -> "B";
            case 2 -> "C";
            default -> "D";
        };
    }

    private String normalizeSkill(String skill) {
        if (skill == null) {
            return "";
        }
        return skill.trim().replace('-', ' ').toLowerCase(Locale.ROOT);
    }

    /** Ensures each skill has exactly 25 unique questions with 6/9/10 difficulty split. */
    private void ensureCatalog() {
        for (String sk : CANONICAL_SKILLS) {
            ensureSkillQuestionSet(sk);
        }
    }

    private void ensureSkillQuestionSet(String skill) {
        List<QuizQuestion> existing = quizQuestionRepository.findBySkillIgnoreCase(skill);
        if (isValidSkillSet(existing)) {
            return;
        }
        quizQuestionRepository.deleteBySkillIgnoreCase(skill);
        seedSkillQuestionSet(skill);
    }

    private boolean isValidSkillSet(List<QuizQuestion> existing) {
        if (existing == null || existing.size() != TARGET_QUIZ_SIZE) {
            return false;
        }
        Set<String> uniqueQuestions = new HashSet<>();
        int b = 0;
        int i = 0;
        int a = 0;
        for (QuizQuestion q : existing) {
            if (q == null || q.getQuestion() == null || q.getQuestion().isBlank()) {
                return false;
            }
            String key = q.getQuestion().trim().toLowerCase(Locale.ROOT);
            if (!uniqueQuestions.add(key)) {
                return false;
            }
            if (q.getOptionsCsv() == null || q.getOptionsCsv().isBlank()) {
                return false;
            }
            String[] opts = q.getOptionsCsv().split("\\|");
            if (opts.length != 4) {
                return false;
            }
            Set<String> optSet = new LinkedHashSet<>();
            for (String opt : opts) {
                if (opt == null || opt.trim().isEmpty()) {
                    return false;
                }
                optSet.add(opt.trim().toLowerCase(Locale.ROOT));
            }
            if (optSet.size() != 4) {
                return false;
            }
            if (q.getCorrectIndex() == null || q.getCorrectIndex() < 0 || q.getCorrectIndex() > 3) {
                return false;
            }
            String d = q.getDifficulty() == null ? "" : q.getDifficulty().trim().toUpperCase(Locale.ROOT);
            if ("BEGINNER".equals(d)) b++;
            else if ("INTERMEDIATE".equals(d)) i++;
            else if ("ADVANCED".equals(d)) a++;
            else return false;
        }
        return b == BEGINNER_TARGET && i == INTERMEDIATE_TARGET && a == ADVANCED_TARGET;
    }

    private void seedSkillQuestionSet(String skill) {
        String label = formatSkillLabel(skill);
        List<String> concepts = conceptsForSkill(skill);
        Set<String> used = new HashSet<>();
        for (int idx = 0; idx < BEGINNER_TARGET; idx++) {
            GeneratedQuestion g = buildQuestion(skill, label, concepts, "BEGINNER", idx, used);
            saveQuestion(skill, g);
        }
        for (int idx = 0; idx < INTERMEDIATE_TARGET; idx++) {
            GeneratedQuestion g = buildQuestion(skill, label, concepts, "INTERMEDIATE", idx, used);
            saveQuestion(skill, g);
        }
        for (int idx = 0; idx < ADVANCED_TARGET; idx++) {
            GeneratedQuestion g = buildQuestion(skill, label, concepts, "ADVANCED", idx, used);
            saveQuestion(skill, g);
        }
    }

    private GeneratedQuestion buildQuestion(String skill, String label, List<String> concepts, String difficulty, int idx, Set<String> used) {
        int conceptIdx = switch (difficulty) {
            case "BEGINNER" -> idx % concepts.size();
            case "INTERMEDIATE" -> (idx + 2) % concepts.size();
            default -> (idx + 4) % concepts.size();
        };
        String c = concepts.get(conceptIdx);
        String c2 = concepts.get((conceptIdx + 1) % concepts.size());
        String c3 = concepts.get((conceptIdx + 2) % concepts.size());

        String question = switch (difficulty) {
            case "BEGINNER" -> beginnerQuestion(label, c, c2, idx);
            case "INTERMEDIATE" -> intermediateQuestion(label, c, c2, c3, idx);
            default -> advancedQuestion(label, c, c2, c3, idx);
        };
        String uniqueKey = question.trim().toLowerCase(Locale.ROOT);
        if (!used.add(uniqueKey)) {
            question = question + " (Skill: " + label + ", Focus: " + c + ")";
            uniqueKey = question.trim().toLowerCase(Locale.ROOT);
            if (!used.add(uniqueKey)) {
                question = question + " #" + (idx + 1);
                used.add(question.trim().toLowerCase(Locale.ROOT));
            }
        }

        return buildOptionSet(skill, label, c, c2, c3, difficulty, idx, question);
    }

    private String beginnerQuestion(String label, String c, String c2, int idx) {
        return switch (idx) {
            case 0 -> "Which statement best defines " + c + " in " + label + "?";
            case 1 -> "When starting with " + label + ", why is " + c + " important?";
            case 2 -> "Which example shows basic understanding of " + c + " in " + label + "?";
            case 3 -> "For a beginner project in " + label + ", where should " + c + " be applied first?";
            case 4 -> "Which confusion between " + c + " and " + c2 + " should beginners avoid in " + label + "?";
            default -> "What is the most accurate beginner-level interpretation of " + c + " in " + label + "?";
        };
    }

    private String intermediateQuestion(String label, String c, String c2, String c3, int idx) {
        return switch (idx) {
            case 0 -> "A team is building a feature in " + label + ". Which use of " + c + " is most appropriate?";
            case 1 -> "In a real " + label + " workflow, when should " + c + " be preferred over " + c2 + "?";
            case 2 -> "A bug appears after introducing " + c + " in " + label + ". What is the best next step?";
            case 3 -> "Which metric best confirms " + c + " is implemented correctly in " + label + "?";
            case 4 -> "How should " + c + " and " + c2 + " work together in production " + label + " systems?";
            case 5 -> "Which trade-off is acceptable when applying " + c + " in a medium-scale " + label + " project?";
            case 6 -> "Which refactor most improves maintainability around " + c + " in " + label + "?";
            case 7 -> "If performance drops in " + label + ", which change related to " + c + " should be tested first?";
            default -> "Which scenario best demonstrates applied understanding of " + c + ", " + c2 + ", and " + c3 + " in " + label + "?";
        };
    }

    private String advancedQuestion(String label, String c, String c2, String c3, int idx) {
        return switch (idx) {
            case 0 -> "In a high-scale " + label + " platform, how should architects balance " + c + " and latency risk?";
            case 1 -> "A security incident touches " + c + " in " + label + ". Which advanced response is strongest?";
            case 2 -> "Which architecture choice minimizes regressions when combining " + c + " with " + c2 + " in " + label + "?";
            case 3 -> "Which failure mode is most likely if " + c + " is over-optimized without " + c3 + " in enterprise " + label + "?";
            case 4 -> "What advanced test strategy best validates " + c + " under realistic " + label + " load?";
            case 5 -> "For long-term evolution of a " + label + " codebase, what governance around " + c + " is best?";
            case 6 -> "A severe latency spike is traced to " + c + " in " + label + ". Which mitigation should lead?";
            case 7 -> "Which observability design most reliably detects " + c + " regressions in " + label + "?";
            case 8 -> "How should teams handle correctness vs scalability when " + c + ", " + c2 + ", and " + c3 + " conflict in " + label + "?";
            default -> "Which expert-level decision avoids hidden production risk while scaling " + c + " in " + label + "?";
        };
    }

    private GeneratedQuestion buildOptionSet(String skill, String label, String c, String c2, String c3, String difficulty, int idx, String question) {
        String correct = correctOptionText(label, c, c2, c3, difficulty);
        List<String> distractors = distractorOptionTexts(skill, label, c, c2, c3, difficulty);
        List<String> options = new ArrayList<>();
        options.add(correct);
        options.addAll(distractors);
        Collections.rotate(options, idx % 4);
        int correctIndex = options.indexOf(correct);
        return new GeneratedQuestion(difficulty, question, options, correctIndex);
    }

    private String correctOptionText(String label, String c, String c2, String c3, String difficulty) {
        return switch (difficulty) {
            case "BEGINNER" -> "Use " + c + " as a foundational concept and validate it with a simple " + label + " example.";
            case "INTERMEDIATE" -> "Apply " + c + " with measurable checks, and coordinate it with " + c2 + " during implementation.";
            default -> "Treat " + c + " as a system-level concern; design guardrails, monitor risk, and balance it with " + c2 + " and " + c3 + ".";
        };
    }

    private List<String> distractorOptionTexts(String skill, String label, String c, String c2, String c3, String difficulty) {
        String s = normalizeSkill(skill);
        if (s.contains("java")) {
            return javaDistractors(c, c2, c3, difficulty);
        }
        if (s.contains("python")) {
            return pythonDistractors(c, c2, c3, difficulty);
        }
        if (s.contains("figma")) {
            return figmaDistractors(c, c2, c3, difficulty);
        }
        if (s.contains("seo")) {
            return seoDistractors(c, c2, c3, difficulty);
        }
        if (s.contains("video")) {
            return videoDistractors(c, c2, c3, difficulty);
        }
        return List.of(
                "Ignore " + c + " and rely only on trial-and-error in " + label + " tasks.",
                "Use " + c2 + " as a substitute for every problem, regardless of context.",
                "Delay decisions about " + c3 + " until after release with no validation."
        );
    }

    private List<String> javaDistractors(String c, String c2, String c3, String difficulty) {
        if ("ADVANCED".equals(difficulty)) {
            return List.of(
                    "Disable profiling and assume JIT always optimizes " + c + " correctly in production.",
                    "Solve all scaling issues by converting every component to static state tied to " + c2 + ".",
                    "Skip testing around " + c3 + " because JVM behavior is always deterministic across environments."
            );
        }
        return List.of(
                "Use mutable global state to simplify " + c + " in all modules.",
                "Treat " + c2 + " as optional documentation only, not executable behavior.",
                "Ignore exceptions related to " + c3 + " and rely on restart-only recovery."
        );
    }

    private List<String> pythonDistractors(String c, String c2, String c3, String difficulty) {
        if ("ADVANCED".equals(difficulty)) {
            return List.of(
                    "Resolve performance issues by replacing all Python " + c + " logic with deep nested loops.",
                    "Avoid tests for " + c2 + " because dynamic typing makes static checks unnecessary.",
                    "Skip environment isolation and deploy with whichever " + c3 + " versions are preinstalled."
            );
        }
        return List.of(
                "Use wildcard imports everywhere and mix unrelated " + c + " concerns in one file.",
                "Assume " + c2 + " behavior is identical across all package versions without pinning.",
                "Debug " + c3 + " issues only after production failures, never during local runs."
        );
    }

    private List<String> figmaDistractors(String c, String c2, String c3, String difficulty) {
        if ("ADVANCED".equals(difficulty)) {
            return List.of(
                    "Avoid component variants and manually duplicate screens to scale " + c + ".",
                    "Ignore prototype logic and rely on verbal explanation instead of validating " + c2 + " flows.",
                    "Skip design-token mapping, then export assets ad hoc when " + c3 + " changes."
            );
        }
        return List.of(
                "Create every frame from scratch and avoid reusable " + c + " patterns.",
                "Treat " + c2 + " feedback as optional and publish without iterative testing.",
                "Ignore handoff constraints and defer " + c3 + " decisions to developers after sign-off."
        );
    }

    private List<String> seoDistractors(String c, String c2, String c3, String difficulty) {
        if ("ADVANCED".equals(difficulty)) {
            return List.of(
                    "Maximize ranking by stuffing keywords and ignoring search intent around " + c + ".",
                    "Build backlinks from any source without relevance checks tied to " + c2 + ".",
                    "Exclude analytics baselines; optimize " + c3 + " using intuition only."
            );
        }
        return List.of(
                "Use identical page titles and meta descriptions for all content regardless of " + c + ".",
                "Ignore technical crawl errors and focus only on social shares for " + c2 + ".",
                "Measure SEO success by impressions alone and skip conversions tied to " + c3 + "."
        );
    }

    private List<String> videoDistractors(String c, String c2, String c3, String difficulty) {
        if ("ADVANCED".equals(difficulty)) {
            return List.of(
                    "Export at maximum bitrate for every platform and ignore delivery constraints around " + c + ".",
                    "Fix pacing issues by adding random transitions instead of restructuring " + c2 + ".",
                    "Skip waveform checks and assume dialogue stays synced during " + c3 + " edits."
            );
        }
        return List.of(
                "Place all clips on one track and ignore organization around " + c + ".",
                "Apply heavy effects first, then decide story structure for " + c2 + ".",
                "Finalize color decisions before fixing cuts and timing in " + c3 + "."
        );
    }

    private List<String> conceptsForSkill(String skill) {
        String s = normalizeSkill(skill);
        if (s.contains("java")) return List.of("JVM lifecycle", "OOP principles", "collections", "streams", "exception handling", "memory management");
        if (s.contains("python")) return List.of("data structures", "functions", "modules", "virtual environments", "pandas workflows", "debugging");
        if (s.contains("figma")) return List.of("frames", "components", "auto layout", "prototyping", "design systems", "handoff");
        if (s.contains("seo")) return List.of("keyword intent", "on-page optimization", "technical crawlability", "backlinks", "search analytics", "content relevance");
        if (s.contains("video")) return List.of("timeline editing", "cuts and transitions", "color correction", "audio sync", "codec export", "story pacing");
        if (s.contains("sql")) return List.of("joins", "aggregation", "indexing", "query planning", "transactions", "normalization");
        if (s.contains("react")) return List.of("component composition", "state management", "hooks", "rendering lifecycle", "performance optimization", "testing");
        if (s.contains("spring")) return List.of("dependency injection", "REST controllers", "persistence", "transaction boundaries", "security filters", "configuration");
        if (s.contains("design")) return List.of("user needs", "hierarchy", "consistency", "interaction patterns", "feedback loops", "accessibility");
        if (s.contains("security")) return List.of("threat modeling", "vulnerability classes", "access control", "network hardening", "incident response", "risk scoring");
        if (s.contains("excel") || s.contains("power bi") || s.contains("tableau")) return List.of("data preparation", "formulas", "visualization", "dashboards", "validation", "reporting");
        if (s.contains("autocad") || s.contains("cad")) return List.of("drawing precision", "layer management", "dimensioning", "annotation", "plot setup", "standards");
        return List.of("core concepts", "tooling", "workflow", "quality checks", "troubleshooting", "optimization");
    }

    private void saveQuestion(String skill, GeneratedQuestion g) {
        QuizQuestion qq = new QuizQuestion();
        qq.setSkill(skill);
        qq.setDifficulty(g.difficulty());
        qq.setQuestion(g.question());
        qq.setOptionsCsv(String.join("|", g.options()));
        qq.setCorrectIndex(g.correctIndex());
        quizQuestionRepository.save(qq);
    }

    private record GeneratedQuestion(String difficulty, String question, List<String> options, int correctIndex) {}
}

