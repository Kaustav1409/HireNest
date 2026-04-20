package com.hirenest.backend.util;

import com.hirenest.backend.entity.CandidateProfile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Normalizes free-text skill phrases and maps them to internal canonical skill ids used by
 * {@link QuizDomainTaxonomy}. Synonyms and surface variants (e.g. ReactJS, HTML5, Node.js) collapse
 * to the same ids so recommendations scale beyond a few hard-coded tokens.
 */
public final class QuizSkillNormalizer {

    private static final Pattern FIELD_SPLIT = Pattern.compile("[,;|]+");
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[\\s,/+]+");
    private static final Set<String> STOPWORDS =
            Set.of("and", "or", "the", "a", "an", "with", "using", "based", "on", "in", "to", "of", "for");

    /** Longer phrases first so "spring boot" wins over "spring". */
    private static final List<Map.Entry<String, Set<String>>> PHRASE_SYNONYMS;

    /** Single-token / short synonym -> canonical id(s). */
    private static final Map<String, Set<String>> TOKEN_SYNONYMS;

    /** Canonical id -> human-readable label for API {@code matchedSkills}. */
    private static final Map<String, String> CANONICAL_LABELS;

    static {
        CANONICAL_LABELS =
                Map.ofEntries(
                        Map.entry("react", "React"),
                        Map.entry("jsx", "JSX"),
                        Map.entry("redux", "Redux"),
                        Map.entry("hooks", "React Hooks"),
                        Map.entry("nextjs", "Next.js"),
                        Map.entry("html", "HTML"),
                        Map.entry("css", "CSS"),
                        Map.entry("javascript", "JavaScript"),
                        Map.entry("typescript", "TypeScript"),
                        Map.entry("nodejs", "Node.js"),
                        Map.entry("npm", "npm"),
                        Map.entry("webpack", "Webpack"),
                        Map.entry("vite", "Vite"),
                        Map.entry("tailwind", "Tailwind CSS"),
                        Map.entry("bootstrap", "Bootstrap"),
                        Map.entry("sass", "Sass"),
                        Map.entry("responsive", "Responsive Design"),
                        Map.entry("dom", "DOM"),
                        Map.entry("http", "HTTP"),
                        Map.entry("json", "JSON"),
                        Map.entry("java", "Java"),
                        Map.entry("oop", "OOP"),
                        Map.entry("collections", "Java Collections"),
                        Map.entry("jdbc", "JDBC"),
                        Map.entry("jvm", "JVM"),
                        Map.entry("kotlin", "Kotlin"),
                        Map.entry("spring_boot", "Spring Boot"),
                        Map.entry("spring", "Spring Framework"),
                        Map.entry("rest", "REST API"),
                        Map.entry("jpa", "JPA"),
                        Map.entry("hibernate", "Hibernate"),
                        Map.entry("maven", "Maven"),
                        Map.entry("gradle", "Gradle"),
                        Map.entry("microservices", "Microservices"),
                        Map.entry("sql", "SQL"),
                        Map.entry("mysql", "MySQL"),
                        Map.entry("postgresql", "PostgreSQL"),
                        Map.entry("mongodb", "MongoDB"),
                        Map.entry("joins", "SQL Joins"),
                        Map.entry("queries", "Queries"),
                        Map.entry("indexing", "Indexing"),
                        Map.entry("dbms", "DBMS"),
                        Map.entry("normalization", "Normalization"),
                        Map.entry("acid", "ACID"),
                        Map.entry("transactions", "Transactions"),
                        Map.entry("er_diagram", "ER Diagram"),
                        Map.entry("python", "Python"),
                        Map.entry("pandas", "pandas"),
                        Map.entry("numpy", "NumPy"),
                        Map.entry("flask", "Flask"),
                        Map.entry("django", "Django"),
                        Map.entry("fastapi", "FastAPI"),
                        Map.entry("dsa", "Data Structures & Algorithms"),
                        Map.entry("arrays", "Arrays"),
                        Map.entry("linked_list", "Linked List"),
                        Map.entry("stack", "Stack"),
                        Map.entry("queue", "Queue"),
                        Map.entry("tree", "Trees"),
                        Map.entry("graph", "Graphs"),
                        Map.entry("heap", "Heap"),
                        Map.entry("hashmap", "Hash Map"),
                        Map.entry("complexity", "Complexity Analysis"),
                        Map.entry("algorithms", "Algorithms"),
                        Map.entry("aptitude", "Aptitude"),
                        Map.entry("reasoning", "Logical Reasoning"));

        List<Map.Entry<String, Set<String>>> phrases = new ArrayList<>();
        addPhrase(phrases, "spring boot", "spring_boot");
        addPhrase(phrases, "springboot", "spring_boot");
        addPhrase(phrases, "react.js", "react");
        addPhrase(phrases, "reactjs", "react");
        addPhrase(phrases, "react native", "react");
        addPhrase(phrases, "node.js", "nodejs");
        addPhrase(phrases, "node js", "nodejs");
        addPhrase(phrases, "express.js", "nodejs");
        addPhrase(phrases, "html5", "html");
        addPhrase(phrases, "html 5", "html");
        addPhrase(phrases, "css3", "css");
        addPhrase(phrases, "css 3", "css");
        addPhrase(phrases, "tailwind css", "tailwind");
        addPhrase(phrases, "material ui", "react");
        addPhrase(phrases, "mui", "react");
        addPhrase(phrases, "vue.js", "javascript");
        addPhrase(phrases, "vuejs", "javascript");
        addPhrase(phrases, "angular.js", "javascript");
        addPhrase(phrases, "angular", "javascript");
        addPhrase(phrases, "ecmascript", "javascript");
        addPhrase(phrases, "es6", "javascript");
        addPhrase(phrases, "es2015", "javascript");
        addPhrase(phrases, "postgres", "postgresql");
        addPhrase(phrases, "postgre sql", "postgresql");
        addPhrase(phrases, "ms sql", "sql");
        addPhrase(phrases, "microsoft sql", "sql");
        addPhrase(phrases, "tsql", "sql");
        addPhrase(phrases, "pl/sql", "sql");
        addPhrase(phrases, "data structure", "dsa");
        addPhrase(phrases, "data structures", "dsa");
        addPhrase(phrases, "ds and algo", "dsa");
        addPhrase(phrases, "dsa", "dsa");
        addPhrase(phrases, "machine learning", "python");
        addPhrase(phrases, "deep learning", "python");
        addPhrase(phrases, "rest api", "rest");
        addPhrase(phrases, "restful", "rest");
        addPhrase(phrases, "linked list", "linked_list");
        addPhrase(phrases, "linked lists", "linked_list");
        phrases.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
        PHRASE_SYNONYMS = Collections.unmodifiableList(phrases);

        TOKEN_SYNONYMS =
                Map.ofEntries(
                        Map.entry("js", Set.of("javascript")),
                        Map.entry("ts", Set.of("typescript")),
                        Map.entry("jsx", Set.of("jsx", "react")),
                        Map.entry("tsx", Set.of("typescript", "react")),
                        Map.entry("es6", Set.of("javascript")),
                        Map.entry("es2015", Set.of("javascript")),
                        Map.entry("node", Set.of("nodejs")),
                        Map.entry("npm", Set.of("npm")),
                        Map.entry("yarn", Set.of("npm")),
                        Map.entry("pnpm", Set.of("npm")),
                        Map.entry("html", Set.of("html")),
                        Map.entry("css", Set.of("css")),
                        Map.entry("scss", Set.of("sass")),
                        Map.entry("less", Set.of("css")),
                        Map.entry("react", Set.of("react")),
                        Map.entry("redux", Set.of("redux")),
                        Map.entry("vue", Set.of("javascript")),
                        Map.entry("java", Set.of("java")),
                        Map.entry("collections", Set.of("collections", "java")),
                        Map.entry("kotlin", Set.of("kotlin")),
                        Map.entry("spring", Set.of("spring", "spring_boot")),
                        Map.entry("springboot", Set.of("spring_boot")),
                        Map.entry("jpa", Set.of("jpa")),
                        Map.entry("hibernate", Set.of("hibernate")),
                        Map.entry("sql", Set.of("sql")),
                        Map.entry("mysql", Set.of("mysql", "sql")),
                        Map.entry("postgres", Set.of("postgresql", "sql")),
                        Map.entry("postgresql", Set.of("postgresql", "sql")),
                        Map.entry("mongo", Set.of("mongodb")),
                        Map.entry("nosql", Set.of("mongodb", "dbms")),
                        Map.entry("python", Set.of("python")),
                        Map.entry("py", Set.of("python")),
                        Map.entry("pandas", Set.of("pandas")),
                        Map.entry("numpy", Set.of("numpy")),
                        Map.entry("flask", Set.of("flask")),
                        Map.entry("django", Set.of("django")),
                        Map.entry("fastapi", Set.of("fastapi")),
                        Map.entry("c++", Set.of("dsa")),
                        Map.entry("cpp", Set.of("dsa")),
                        Map.entry("c", Set.of("dsa")),
                        Map.entry("go", Set.of("dsa")),
                        Map.entry("rust", Set.of("dsa")),
                        Map.entry("dsa", Set.of("dsa")),
                        Map.entry("algo", Set.of("algorithms", "dsa")),
                        Map.entry("algorithms", Set.of("algorithms", "dsa")),
                        Map.entry("leetcode", Set.of("dsa")),
                        Map.entry("array", Set.of("arrays", "dsa")),
                        Map.entry("arrays", Set.of("arrays", "dsa")),
                        Map.entry("tree", Set.of("tree", "dsa")),
                        Map.entry("trees", Set.of("tree", "dsa")),
                        Map.entry("graph", Set.of("graph", "dsa")),
                        Map.entry("graphs", Set.of("graph", "dsa")),
                        Map.entry("stack", Set.of("stack", "dsa")),
                        Map.entry("queue", Set.of("queue", "dsa")),
                        Map.entry("heap", Set.of("heap", "dsa")),
                        Map.entry("hashmap", Set.of("hashmap", "dsa")),
                        Map.entry("map", Set.of("hashmap", "dsa")),
                        Map.entry("big-o", Set.of("complexity")),
                        Map.entry("complexity", Set.of("complexity", "dsa")),
                        Map.entry("dbms", Set.of("dbms")),
                        Map.entry("database", Set.of("dbms", "sql")),
                        Map.entry("rdbms", Set.of("dbms", "sql")),
                        Map.entry("erd", Set.of("er_diagram")),
                        Map.entry("aptitude", Set.of("aptitude")),
                        Map.entry("reasoning", Set.of("reasoning", "aptitude")),
                        Map.entry("quant", Set.of("aptitude")),
                        Map.entry("verbal", Set.of("aptitude")),
                        Map.entry("jwt", Set.of("http", "json")),
                        Map.entry("oauth", Set.of("http")),
                        Map.entry("cors", Set.of("http")),
                        Map.entry("ajax", Set.of("javascript", "dom")),
                        Map.entry("jquery", Set.of("javascript", "dom")),
                        Map.entry("webpack", Set.of("webpack")),
                        Map.entry("vite", Set.of("vite")),
                        Map.entry("tailwind", Set.of("tailwind")),
                        Map.entry("bootstrap", Set.of("bootstrap")));
    }

    private QuizSkillNormalizer() {}

    private static void addPhrase(List<Map.Entry<String, Set<String>>> phrases, String phrase, String canonical) {
        String key = surfaceKey(phrase);
        if (key.isEmpty()) {
            return;
        }
        phrases.add(Map.entry(key, Set.of(canonical)));
    }

    /**
     * Merges {@link CandidateProfile#getSkills()}, {@link CandidateProfile#getExtractedSkills()}, and
     * {@link CandidateProfile#getPreferredRoles()} (same normalization as skills) into canonical ids.
     */
    public static LinkedHashSet<String> canonicalSkillSetFromProfile(CandidateProfile profile) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (profile == null) {
            return out;
        }
        ingestField(profile.getSkills(), out);
        ingestField(profile.getExtractedSkills(), out);
        ingestPreferredRolesField(profile.getPreferredRoles(), out);
        return out;
    }

    public static boolean hasSkillText(CandidateProfile profile) {
        if (profile == null) {
            return false;
        }
        return notBlank(profile.getSkills())
                || notBlank(profile.getExtractedSkills())
                || notBlank(profile.getPreferredRoles());
    }

    /** Preferred roles often contain technology names (e.g. &quot;React Developer&quot;) â€” treat like skill phrases. */
    private static void ingestPreferredRolesField(String field, LinkedHashSet<String> sink) {
        if (field == null || field.isBlank()) {
            return;
        }
        for (String segment : FIELD_SPLIT.split(field)) {
            String s = segment.trim();
            if (s.isEmpty()) {
                continue;
            }
            sink.addAll(expandSegmentToCanonicals(s));
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static void ingestField(String field, LinkedHashSet<String> sink) {
        if (field == null || field.isBlank()) {
            return;
        }
        for (String segment : FIELD_SPLIT.split(field)) {
            String s = segment.trim();
            if (s.isEmpty()) {
                continue;
            }
            sink.addAll(expandSegmentToCanonicals(s));
        }
    }

    /**
     * Maps a single resume/profile segment (e.g. "React.js", "Spring Boot 3") to zero or more canonical
     * skill ids.
     */
    public static Set<String> expandSegmentToCanonicals(String segment) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (segment == null || segment.isBlank()) {
            return out;
        }
        String key = surfaceKey(segment);
        if (key.isEmpty()) {
            return out;
        }
        for (Map.Entry<String, Set<String>> e : PHRASE_SYNONYMS) {
            if (phraseMatches(key, e.getKey())) {
                out.addAll(e.getValue());
            }
        }
        String[] parts = TOKEN_SPLIT.split(key);
        List<String> tokens = new ArrayList<>();
        for (String p : parts) {
            String t = p.replaceAll("^[^a-z0-9+#]+|[^a-z0-9+#]+$", "");
            if (t.length() < 1 || STOPWORDS.contains(t)) {
                continue;
            }
            tokens.add(t);
        }
        for (int i = 0; i < tokens.size(); i++) {
            String t = tokens.get(i);
            addTokenMatches(t, out);
            if (i + 1 < tokens.size()) {
                String bigram = tokens.get(i) + " " + tokens.get(i + 1);
                for (Map.Entry<String, Set<String>> e : PHRASE_SYNONYMS) {
                    if (phraseMatches(bigram, e.getKey())) {
                        out.addAll(e.getValue());
                    }
                }
            }
        }
        return out;
    }

    private static void addTokenMatches(String token, Set<String> sink) {
        Set<String> direct = TOKEN_SYNONYMS.get(token);
        if (direct != null) {
            sink.addAll(direct);
        }
    }

    /** Single-token phrase keys match only as equals; multi-word keys may match as substrings of the segment. */
    private static boolean phraseMatches(String fullKey, String phraseKey) {
        if (fullKey.equals(phraseKey)) {
            return true;
        }
        if (!phraseKey.contains(" ")) {
            return false;
        }
        return fullKey.contains(phraseKey);
    }

    private static String surfaceKey(String s) {
        if (s == null) {
            return "";
        }
        String x = s.trim().toLowerCase(Locale.ROOT);
        x = x.replace('\u00a0', ' ');
        x = x.replaceAll("[._]+", " ");
        x = x.replaceAll("\\s+", " ").trim();
        return x;
    }

    public static String labelForCanonical(String canonicalId) {
        if (canonicalId == null || canonicalId.isBlank()) {
            return "";
        }
        String fallback = canonicalId.replace('_', ' ');
        if (fallback.isEmpty()) {
            return "";
        }
        return CANONICAL_LABELS.getOrDefault(
                canonicalId,
                fallback.substring(0, 1).toUpperCase(Locale.ROOT)
                        + (fallback.length() > 1 ? fallback.substring(1) : ""));
    }

    public static List<String> labelsForCanonicals(Iterable<String> canonicalIds) {
        List<String> labels = new ArrayList<>();
        for (String id : canonicalIds) {
            String lb = labelForCanonical(id);
            if (!lb.isEmpty()) {
                labels.add(lb);
            }
        }
        return labels;
    }
}

