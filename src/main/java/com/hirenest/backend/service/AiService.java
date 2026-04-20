package com.hirenest.backend.service;

import com.hirenest.backend.dto.AiDtos.ChatResponse;
import com.hirenest.backend.dto.AiDtos.JobMatchSnippet;
import com.hirenest.backend.dto.AiDtos.JobSeekerAiContext;
import com.hirenest.backend.dto.JobDtos.LearningResourceDto;
import com.hirenest.backend.exception.BadRequestException;
import com.hirenest.backend.util.LearningResourceMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Demo-ready assistant: intent classification + templates over real dashboard data (no external LLM).
 */
@Service
public class AiService {

    private final PromptBuilderService promptBuilderService;

    public AiService(PromptBuilderService promptBuilderService) {
        this.promptBuilderService = promptBuilderService;
    }

    public ChatResponse chat(Long userId, String message) {
        requireJobSeekerSelf(userId);
        if (message == null || message.isBlank()) {
            throw new BadRequestException("Message is required");
        }
        String trimmed = message.trim();
        JobSeekerAiContext ctx = promptBuilderService.buildJobSeekerContext(userId);
        String intent = classifyIntent(trimmed);
        ChatResponse out = new ChatResponse();
        out.intent = intent;
        out.supportingData = buildSupportingData(ctx);

        switch (intent) {
            case "JOBS_FIT":
                out.aiReply = replyJobsFit(ctx);
                break;
            case "MISSING_SKILLS":
                out.aiReply = replyMissingSkills(ctx);
                break;
            case "LEARN_NEXT":
                out.aiReply = replyLearnNext(ctx);
                break;
            case "ROADMAP":
                out.aiReply = replyRoadmap(ctx);
                break;
            case "WEAK_AREAS":
                out.aiReply = replyWeakAreas(ctx);
                break;
            case "PROJECTS":
                out.aiReply = replyProjects(ctx);
                break;
            case "GREETING":
                out.aiReply = replyGreeting(ctx);
                break;
            case "HELP":
                out.aiReply = replyHelp();
                break;
            default:
                out.intent = "GENERAL";
                out.aiReply = replyGeneral(ctx, trimmed);
        }
        return out;
    }

    private void requireJobSeekerSelf(Long userId) {
        if (userId == null) {
            throw new BadRequestException("userId is required");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new AccessDeniedException("Sign in to use the assistant.");
        }
        String principal = String.valueOf(auth.getPrincipal());
        if (!principal.equals(String.valueOf(userId))) {
            throw new AccessDeniedException("You can only use the assistant for your own account.");
        }
    }

    private String classifyIntent(String q) {
        String s = q.toLowerCase(Locale.ROOT);
        if (s.matches("^(hi|hey|hello)\\b.*") || s.startsWith("good morning")) {
            return "GREETING";
        }
        if (matchesAny(s, "help", "what can you", "how do you")) {
            return "HELP";
        }
        if ((containsAny(s, "job", "role", "position", "opening") && containsAny(s, "fit", "match", "recommend", "which", "best", "suit"))
                || (containsAny(s, "which job", "what job"))) {
            return "JOBS_FIT";
        }
        if (containsAny(s, "missing", "gap", "don't have", "do not have", "lack", "not on my profile")) {
            return "MISSING_SKILLS";
        }
        if (containsAny(s, "learn next", "should i learn", "what to learn", "study next", "focus on")) {
            return "LEARN_NEXT";
        }
        if (containsAny(s, "roadmap", "learning path", "career path", "plan", "steps to")) {
            return "ROADMAP";
        }
        if (containsAny(s, "weak", "weakness", "struggle", "improve", "bottleneck")) {
            return "WEAK_AREAS";
        }
        if (containsAny(s, "project", "portfolio", "build", "practice")) {
            return "PROJECTS";
        }
        return "GENERAL";
    }

    private boolean matchesAny(String s, String... needles) {
        for (String n : needles) {
            if (s.startsWith(n.trim()) || s.contains(n)) return true;
        }
        return false;
    }

    private boolean containsAny(String s, String... needles) {
        for (String n : needles) {
            if (s.contains(n)) return true;
        }
        return false;
    }

    private Map<String, Object> buildSupportingData(JobSeekerAiContext ctx) {
        Map<String, Object> m = new HashMap<>();
        m.put("profileCompletionPercent", ctx.profileCompletionPercent);
        m.put("matchedJobCount", ctx.matchedJobCount);
        m.put("aggregatedMissingSkills", ctx.aggregatedMissingSkills);
        m.put("latestQuizScore", ctx.latestQuizScore);
        m.put("topMatchTitles", ctx.topMatches.stream().map(t -> t.title).toList());
        return m;
    }

    private String replyGreeting(JobSeekerAiContext ctx) {
        return "Hi! I am your HireNest assistant. I use your live profile, job matches, quiz scores, and learning data. "
                + "Ask me about jobs that fit you, skills to learn, or a roadmap. "
                + "Your profile is about " + ctx.profileCompletionPercent + "% complete"
                + (ctx.matchedJobCount > 0 ? ", and I see " + ctx.matchedJobCount + " recommended roles." : ".");
    }

    private String replyHelp() {
        return "Try asking: \n"
                + "â€¢ Which jobs fit my profile?\n"
                + "â€¢ What skills am I missing?\n"
                + "â€¢ What should I learn next?\n"
                + "â€¢ Suggest a roadmap for my target role\n"
                + "â€¢ Explain my weak areas\n"
                + "â€¢ Suggest projects for my gaps";
    }

    private String replyJobsFit(JobSeekerAiContext ctx) {
        if (ctx.topMatches.isEmpty()) {
            return "There are no ranked matches yet. Add skills, preferred roles, and location to your profile, "
                    + "then check Recommended Jobs â€” matches update from your profile automatically.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Here are your strongest current matches (from HireNest matching):\n");
        int i = 1;
        for (JobMatchSnippet m : ctx.topMatches) {
            double score = m.matchScore != null ? m.matchScore : 0;
            sb.append(i++).append(". ").append(nullTo(m.title)).append(" at ").append(nullTo(m.companyName))
                    .append(" â€” about ").append(Math.round(score)).append("% match")
                    .append(m.matchLabel != null ? " (" + m.matchLabel + ")" : "")
                    .append(".\n");
        }
        if (!ctx.preferredRoles.isBlank()) {
            sb.append("Your preferred roles include: ").append(ctx.preferredRoles).append(". ");
        }
        sb.append("Open the Recommended Jobs tab for full detail, filters, and apply actions.");
        return sb.toString();
    }

    private String replyMissingSkills(JobSeekerAiContext ctx) {
        if (ctx.aggregatedMissingSkills.isEmpty()) {
            return "Across current job matches, no recurring missing skills were listed â€” either your profile aligns well, "
                    + "or jobs have few required skills defined. Keep your skills list updated for finer gaps.";
        }
        return "Aggregated from your recommended jobs, skills that often appear as gaps include: "
                + String.join(", ", ctx.aggregatedMissingSkills)
                + ". Strengthening these will improve match scores. See each job card for role-specific missing skills.";
    }

    private String replyLearnNext(JobSeekerAiContext ctx) {
        if (ctx.topMissingForLearning.isBlank()) {
            return "No consolidated gap list yet. Try adding more skills to your profile or browse Recommended Jobs â€” "
                    + "missing skills per job drive learning suggestions.";
        }
        String first = ctx.aggregatedMissingSkills.get(0);
        String beginner = LearningResourceMapper.linkFor(first);
        return "Based on your matches, prioritize: " + ctx.topMissingForLearning + ".\n"
                + "Start with \"" + first + "\" â€” here's a beginner-friendly entry point: " + beginner + "\n"
                + "Use the Learning Roadmap section on each job card for Beginner / Intermediate / Advanced links.";
    }

    private String replyRoadmap(JobSeekerAiContext ctx) {
        if (ctx.aggregatedMissingSkills.isEmpty()) {
            return "Set a target role in your preferred roles and ensure jobs are posted â€” your roadmap will come from "
                    + "missing skills on those matches. You can also take the Java quiz to benchmark fundamentals.";
        }
        String skill = ctx.aggregatedMissingSkills.get(0);
        var roadmap = LearningResourceMapper.roadmapFor(skill);
        StringBuilder sb = new StringBuilder();
        sb.append("Sample roadmap for \"").append(roadmap.skillName).append("\" (from your top gaps):\n");
        appendLevel(sb, "Beginner", roadmap.beginnerResources);
        appendLevel(sb, "Intermediate", roadmap.intermediateResources);
        appendLevel(sb, "Advanced", roadmap.advancedResources);
        sb.append("Repeat this pattern for other missing skills using the dashboards learning cards.");
        return sb.toString();
    }

    private void appendLevel(StringBuilder sb, String label, List<LearningResourceDto> items) {
        sb.append("â€¢ ").append(label).append(": ");
        if (items == null || items.isEmpty()) {
            sb.append("(see platform learning links)\n");
            return;
        }
        boolean first = true;
        for (var r : items) {
            if (!first) sb.append("; ");
            first = false;
            sb.append(r.title).append(" â€” ").append(r.url);
        }
        sb.append("\n");
    }

    private String replyWeakAreas(JobSeekerAiContext ctx) {
        StringBuilder sb = new StringBuilder();
        if (!ctx.missingProfileItems.isEmpty()) {
            sb.append("Profile gaps: ").append(String.join("; ", ctx.missingProfileItems)).append("\n");
        }
        if (ctx.quizAttemptCount > 0) {
            sb.append("Latest quiz score: ").append(Math.round(ctx.latestQuizScore)).append("% (average ")
                    .append(Math.round(ctx.avgQuizScore)).append("% across attempts). ");
            if (ctx.latestQuizScore < 60) {
                sb.append("Revisit core Java concepts and retake the quiz when ready. ");
            }
        } else {
            sb.append("No quiz attempts yet â€” the assessment helps expose weak fundamentals. ");
        }
        if (!ctx.aggregatedMissingSkills.isEmpty()) {
            sb.append("Skill gaps from jobs: ").append(String.join(", ", ctx.aggregatedMissingSkills)).append(".");
        } else {
            sb.append("Skill gaps will show once matches list missing requirements.");
        }
        return sb.toString();
    }

    private String replyProjects(JobSeekerAiContext ctx) {
        if (ctx.aggregatedMissingSkills.isEmpty()) {
            return "Once missing skills appear on your matches, build small projects that exercise those technologies â€” "
                    + "e.g. a CRUD app for backend gaps, or a UI for front-end gaps.";
        }
        List<String> top = ctx.aggregatedMissingSkills.stream().limit(3).toList();
        return "Project ideas tied to your current gaps (" + String.join(", ", top) + "):\n"
                + "â€¢ Mini REST API + database if backend/database skills are missing.\n"
                + "â€¢ Single-page app with routing if React/JS gaps appear.\n"
                + "â€¢ Containerize an app with Docker if DevOps skills are listed as gaps.\n"
                + "Publish to GitHub with a README explaining stack and what you learned.";
    }

    private String replyGeneral(JobSeekerAiContext ctx, String original) {
        return "Here is a quick snapshot from your HireNest data:\n"
                + "â€¢ Profile completion: " + ctx.profileCompletionPercent + "%\n"
                + "â€¢ Recommended jobs: " + ctx.matchedJobCount + "\n"
                + "â€¢ Latest quiz score: " + (ctx.quizAttemptCount == 0 ? "not taken yet" : Math.round(ctx.latestQuizScore) + "%")
                + "\n"
                + "â€¢ Top skills to consider from matches: "
                + (ctx.topMissingForLearning.isEmpty() ? "none listed yet" : ctx.topMissingForLearning)
                + "\n\n"
                + "Ask me specifically about jobs, missing skills, learning next steps, or projects â€” "
                + "or type \"help\" for examples. Your question was: \"" + original + "\".";
    }

    private static String nullTo(String s) {
        return s == null || s.isBlank() ? "Unknown" : s;
    }
}

