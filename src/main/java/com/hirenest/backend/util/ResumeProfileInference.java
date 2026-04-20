package com.hirenest.backend.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Derives profile hints from raw resume text and detected skill names (canonical).
 */
public final class ResumeProfileInference {
    private ResumeProfileInference() {}

    private static final int BIO_MAX_LEN = 380;

    private static final Pattern EXP_YEARS = Pattern.compile(
            "\\b(\\d{1,2})\\s*(?:\\+)?\\s*(?:year|years|yrs?)(?:\\s+of)?\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern LOCATION_LINE = Pattern.compile(
            "(?i)(?:^|\\n)\\s*(?:location|address|based\\s+in|residing\\s+in|city|current\\s+location)\\s*[:\\-]\\s*([^\\n]+)");

    private static final Pattern COMMA_LOCATION = Pattern.compile(
            "(?i)^\\s*([A-Z][a-zA-Z]+(?:\\s+[A-Z][a-zA-Z]+){0,3})\\s*,\\s*(India|USA|United States|UK|Canada|Australia)\\s*$");

    /** Lowercase tokens for fuzzy city match in normalized text. */
    private static final String[] KNOWN_LOCATIONS = {
            "kolkata", "mumbai", "delhi", "new delhi", "bengaluru", "bangalore",
            "hyderabad", "chennai", "pune", "ahmedabad", "jaipur", "noida", "gurgaon",
            "gurugram",             "kochi", "indore", "bhubaneswar", "lucknow"
    };

    /**
     * Rule-based role from canonical skill names (order matters: first match wins).
     */
    public static String inferPreferredRole(List<String> canonicalSkills) {
        if (canonicalSkills == null || canonicalSkills.isEmpty()) {
            return "";
        }
        Set<String> lower = new LinkedHashSet<>();
        for (String s : canonicalSkills) {
            if (s != null && !s.isBlank()) {
                lower.add(s.trim().toLowerCase(Locale.ROOT));
            }
        }

        if ((lower.contains("kotlin") || lower.contains("java")) && lower.contains("android")) {
            return "Android Developer";
        }
        if (lower.contains("python") && (lower.contains("pandas") || lower.contains("sql")) && lower.contains("mysql")) {
            return "Data Analyst";
        }
        if (lower.contains("python") && lower.contains("pandas")) {
            return "Data Analyst";
        }
        if (lower.contains("html") && lower.contains("css")
                && (lower.contains("javascript") || lower.contains("typescript"))
                && lower.contains("react")) {
            return "Frontend Developer";
        }
        if (lower.contains("java") && lower.contains("spring boot") && (lower.contains("sql") || lower.contains("mysql"))) {
            return "Backend Developer";
        }
        if (lower.contains("react") && lower.contains("node.js")) {
            return "Full Stack Developer";
        }
        if (lower.contains("python") && lower.contains("sql")) {
            return "Data Analyst";
        }
        if (lower.contains("html") && lower.contains("css") && lower.contains("javascript")) {
            return "Frontend Developer";
        }
        if (lower.contains("java") || lower.contains("spring boot") || lower.contains("spring")) {
            return "Backend Developer";
        }
        if (lower.contains("react") || lower.contains("angular") || lower.contains("vue.js")) {
            return "Frontend Developer";
        }
        return "Software Developer";
    }

    /**
     * Best-effort location: labeled lines first, then known city tokens.
     */
    public static String detectLocation(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }
        String norm = normalizeScan(rawText);
        Matcher lm = LOCATION_LINE.matcher(rawText);
        if (lm.find()) {
            String candidate = cleanLocationCandidate(lm.group(1));
            if (!candidate.isBlank()) {
                return candidate;
            }
        }
        for (String line : rawText.split("[\\r\\n]+")) {
            String t = line.trim();
            if (t.length() < 8 || t.length() > 120) {
                continue;
            }
            Matcher cm = COMMA_LOCATION.matcher(t);
            if (cm.find()) {
                return cleanLocationCandidate(cm.group(1).trim() + ", " + cm.group(2));
            }
        }
        for (String city : KNOWN_LOCATIONS) {
            if (containsWholeWord(norm, city)) {
                return titleCaseLocation(city);
            }
        }
        return "";
    }

    /**
     * First plausible total years of experience (e.g. "2 years", "1+ years").
     */
    public static Integer detectExperienceYears(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }
        Matcher m = EXP_YEARS.matcher(rawText);
        if (m.find()) {
            try {
                int y = Integer.parseInt(m.group(1));
                if (y >= 0 && y <= 60) {
                    return y;
                }
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return null;
    }

    /**
     * Short professional summary from skills + resume snippet.
     */
    public static String generateBio(String rawText, List<String> canonicalSkills) {
        List<String> skills = canonicalSkills == null ? List.of() : new ArrayList<>(canonicalSkills);
        String skillCsv = String.join(", ", skills);
        if (!skillCsv.isBlank()) {
            String intro = "Professional with skills including " + skillCsv + ".";
            String extra = extractResumeSnippet(rawText, 180);
            if (extra != null && !extra.isBlank()) {
                String combined = intro + " " + extra;
                return truncate(combined, BIO_MAX_LEN);
            }
            return truncate(intro, BIO_MAX_LEN);
        }
        String snippet = extractResumeSnippet(rawText, BIO_MAX_LEN);
        return snippet != null ? snippet : "";
    }

    private static String extractResumeSnippet(String rawText, int maxLen) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }
        String cleaned = rawText
                .replaceAll("[\\r\\t]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        // Skip very short noise
        if (cleaned.length() < 20) {
            return "";
        }
        // Prefer content after common headers
        Pattern skipHeader = Pattern.compile(
                "(?i)(?:resume|curriculum vitae|cv)\\s*of\\s*[^\\n]{0,80}\\n");
        Matcher sm = skipHeader.matcher(cleaned);
        if (sm.find()) {
            cleaned = cleaned.substring(Math.min(sm.end(), cleaned.length())).trim();
        }
        int end = Math.min(maxLen, cleaned.length());
        String slice = cleaned.substring(0, end);
        int lastPeriod = slice.lastIndexOf('.');
        if (lastPeriod > 40) {
            slice = slice.substring(0, lastPeriod + 1);
        }
        return slice.trim();
    }

    private static String cleanLocationCandidate(String s) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("[|â€¢]+", " ").trim();
        t = t.replaceAll("\\s+", " ");
        if (t.length() > 100) {
            t = t.substring(0, 100).trim();
        }
        return t;
    }

    private static String titleCaseLocation(String lower) {
        String[] parts = lower.split("\\s+");
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

    private static String normalizeScan(String s) {
        String norm = Normalizer.normalize(s, Normalizer.Form.NFKC);
        norm = norm.toLowerCase(Locale.ROOT);
        norm = norm.replaceAll("[^a-z0-9\\s]", " ");
        norm = norm.replaceAll("\\s+", " ").trim();
        return norm;
    }

    private static boolean containsWholeWord(String haystack, String word) {
        if (haystack == null || word == null || word.isBlank()) {
            return false;
        }
        return Pattern.compile("(^| )" + Pattern.quote(word) + "( |$)").matcher(haystack).find();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 1).trim() + "â€¦";
    }
}

