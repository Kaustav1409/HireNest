package com.hirenest.backend.controller;

import com.hirenest.backend.dto.QuizDtos.QuizQuestionResponse;
import com.hirenest.backend.dto.QuizDtos.QuizRecommendedAssessment;
import com.hirenest.backend.dto.QuizDtos.QuizResultResponse;
import com.hirenest.backend.dto.QuizDtos.QuizSubmission;
import com.hirenest.backend.dto.QuizDtos.QuizAttemptStartRequest;
import com.hirenest.backend.dto.QuizDtos.QuizAttemptStartResponse;
import com.hirenest.backend.dto.QuizDtos.QuizAttemptHeartbeatRequest;
import com.hirenest.backend.dto.AuthDtos.SimpleMessageResponse;
import com.hirenest.backend.dto.QuizDtos.QuizAttemptViolationRequest;
import com.hirenest.backend.dto.QuizDtos.AssessmentOverviewItem;
import com.hirenest.backend.dto.QuizDtos.UserSkillProfileItem;
import com.hirenest.backend.service.QuizService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quiz")
@CrossOrigin
public class QuizController {
    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    /** Supported assessment domains (skill slugs). */
    @GetMapping("/domains")
    public List<String> listDomains() {
        return quizService.listDomains();
    }

    /** Personalized quiz domain suggestions from profile + resume-extracted skills. */
    @GetMapping("/recommended/{userId}")
    public List<QuizRecommendedAssessment> recommended(@PathVariable Long userId) {
        return quizService.recommendedAssessments(userId);
    }

    @GetMapping("/overview/{userId}")
    public List<AssessmentOverviewItem> overview(@PathVariable Long userId) {
        return quizService.assessmentOverview(userId);
    }

    @GetMapping("/skill-profile/{userId}")
    public List<UserSkillProfileItem> skillProfile(@PathVariable Long userId) {
        return quizService.userSkillProfile(userId);
    }

    /**
     * Returns a structured dataset view for requested skills.
     * Example: /api/quiz/sample-dataset?skills=java&skills=python&skills=figma
     */
    @GetMapping("/sample-dataset")
    public Map<String, List<Map<String, Object>>> sampleDataset(
            @RequestParam(required = false) List<String> skills) {
        return quizService.sampleQuestionDataset(skills);
    }

    /**
     * Questions for a skill/domain using query parameters (e.g. {@code /api/quiz?skill=Java&difficulty=Beginner}).
     * Same behavior as {@link #quizByPath(String, String)}.
     */
    @GetMapping
    public List<QuizQuestionResponse> quizByQuery(
            @RequestParam String skill,
            @RequestParam(required = false) String difficulty) {
        return quizService.quiz(skill, difficulty);
    }

    /**
     * Questions for a skill/domain. Optional {@code difficulty}: Beginner, Intermediate, Advanced.
     * Omit difficulty to include all questions for that domain (including legacy rows without difficulty).
     */
    @GetMapping("/{skill}")
    public List<QuizQuestionResponse> quizByPath(
            @PathVariable String skill,
            @RequestParam(required = false) String difficulty) {
        return quizService.quiz(skill, difficulty);
    }

    @PostMapping("/submit")
    public QuizResultResponse submitQuiz(@RequestBody QuizSubmission submission) {
        return quizService.submitQuiz(submission);
    }

    @PostMapping("/attempt/start")
    public QuizAttemptStartResponse startAttempt(@RequestBody QuizAttemptStartRequest request) {
        return quizService.startAttempt(request);
    }

    @PostMapping("/attempt/heartbeat")
    public SimpleMessageResponse heartbeat(@RequestBody QuizAttemptHeartbeatRequest request) {
        quizService.heartbeat(request);
        SimpleMessageResponse out = new SimpleMessageResponse();
        out.message = "ok";
        return out;
    }

    @PostMapping("/attempt/violation")
    public SimpleMessageResponse violation(@RequestBody QuizAttemptViolationRequest request) {
        quizService.reportViolation(request);
        SimpleMessageResponse out = new SimpleMessageResponse();
        out.message = "attempt invalidated";
        return out;
    }
}

