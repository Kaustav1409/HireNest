package com.hirenest.backend.util;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Resume PDF parsing and skill extraction using Apache PDFBox.
 *
 * <p>Skills are matched via a canonical name + synonym list, with phrase padding (handles comma/slash
 * separated lists) and a token pass for single-word technologies. Duplicates are removed while preserving
 * detection order.
 */
public final class ResumeParserUtil {
    private ResumeParserUtil() {}

    private static final Map<String, List<String>> SKILL_ALIASES = buildSkillAliases();

    /**
     * Extracts plain text from a PDF byte stream. Returns an empty string if the PDF has no extractable text
     * layer (e.g. scanned image only); does not throw for empty text so callers can still persist the file.
     */
    public static String extractTextFromPdf(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        if (bytes.length == 0) {
            return "";
        }
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setLineSeparator("\n");
            stripper.setWordSeparator(" ");
            String text = stripper.getText(doc);
            if (text == null || text.isBlank()) {
                return "";
            }
            return text;
        }
    }

    public static List<String> extractSkills(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }

        String text = normalizeForSearch(rawText);
        if (text.isBlank()) {
            return List.of();
        }

        Set<String> detected = new LinkedHashSet<>();

        String padded = " " + text + " ";

        for (Map.Entry<String, List<String>> e : SKILL_ALIASES.entrySet()) {
            String canonical = e.getKey();
            for (String alias : e.getValue()) {
                String na = normalizeForSearch(alias);
                if (na.isBlank()) {
                    continue;
                }
                if (na.contains(" ")) {
                    if (padded.contains(" " + na + " ")) {
                        detected.add(canonical);
                        break;
                    }
                } else if (matchesSingleToken(padded, na)) {
                    detected.add(canonical);
                    break;
                }
            }
        }

        tokenPass(text, detected);

        return new ArrayList<>(detected);
    }

    /**
     * Single-token alias: avoid matching as substring inside longer words (e.g. "java" in "javascript" handled
     * by requiring non-alphanumeric neighbors; "c" is excluded from token pass via alias list).
     */
    private static boolean matchesSingleToken(String paddedText, String token) {
        if (token.length() <= 1) {
            return false;
        }
        String escaped = Pattern.quote(token);
        Pattern p = Pattern.compile("(^|[^a-z0-9+.#])" + escaped + "([^a-z0-9+.#]|$)");
        return p.matcher(paddedText).find();
    }

    /** Second pass: split on common separators and map raw tokens to canonical skills. */
    private static void tokenPass(String normalizedText, Set<String> detected) {
        String[] chunks = normalizedText.split("[,;|/â€¢Â·\n\t]+");
        for (String chunk : chunks) {
            for (String word : chunk.trim().split("\\s+")) {
                String w = word.replaceAll("^[^a-z0-9+.#]+|[^a-z0-9+.#]+$", "");
                if (w.length() < 2) {
                    continue;
                }
                String canon = TOKEN_TO_CANONICAL.get(w);
                if (canon != null) {
                    detected.add(canon);
                }
            }
        }
    }

    private static final Map<String, String> TOKEN_TO_CANONICAL = buildTokenToCanonical();

    private static Map<String, String> buildTokenToCanonical() {
        Map<String, String> m = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : SKILL_ALIASES.entrySet()) {
            String canonical = e.getKey();
            for (String alias : e.getValue()) {
                String na = normalizeForSearch(alias);
                if (!na.isBlank() && !na.contains(" ")) {
                    m.putIfAbsent(na, canonical);
                }
            }
        }
        m.putIfAbsent("postgres", "PostgreSQL");
        m.putIfAbsent("postgresql", "PostgreSQL");
        m.putIfAbsent("tailwind", "Tailwind CSS");
        m.putIfAbsent("tailwindcss", "Tailwind CSS");
        m.putIfAbsent("nextjs", "Next.js");
        m.putIfAbsent("next.js", "Next.js");
        m.putIfAbsent("hibernate", "Hibernate");
        m.putIfAbsent("jpa", "JPA");
        m.putIfAbsent("jdbc", "JDBC");
        m.putIfAbsent("dsa", "Data Structures");
        m.putIfAbsent("ml", "Machine Learning");
        m.putIfAbsent("go", "Go");
        return Map.copyOf(m);
    }

    private static Map<String, List<String>> buildSkillAliases() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put("Java", List.of("java"));
        m.put("Spring", List.of("spring framework", "spring mvc", "spring"));
        m.put("Spring Boot", List.of("spring boot", "springboot", "sb3"));
        m.put("Hibernate", List.of("hibernate"));
        m.put("JPA", List.of("jpa", "java persistence"));
        m.put("JDBC", List.of("jdbc"));
        m.put("Maven", List.of("maven"));
        m.put("Gradle", List.of("gradle"));
        m.put("JUnit", List.of("junit"));
        m.put("MySQL", List.of("mysql"));
        m.put("PostgreSQL", List.of("postgresql", "postgres", "psql"));
        m.put("SQL", List.of("sql", "structured query language"));
        m.put("MongoDB", List.of("mongodb", "mongo db", "mongo"));
        m.put("Redis", List.of("redis"));
        m.put("React", List.of("react", "reactjs", "react.js"));
        m.put("Next.js", List.of("next.js", "nextjs"));
        m.put("JavaScript", List.of("javascript", "ecmascript"));
        m.put("TypeScript", List.of("typescript", "ts"));
        m.put("HTML", List.of("html", "html5"));
        m.put("CSS", List.of("css", "css3"));
        m.put("Tailwind CSS", List.of("tailwind", "tailwind css", "tailwindcss"));
        m.put("Bootstrap", List.of("bootstrap"));
        m.put("Sass", List.of("sass", "scss"));
        m.put("Node.js", List.of("node.js", "nodejs", "node js"));
        m.put("Express.js", List.of("express", "express.js", "expressjs"));
        m.put("Python", List.of("python"));
        m.put("Django", List.of("django"));
        m.put("Flask", List.of("flask"));
        m.put("FastAPI", List.of("fastapi"));
        m.put("pandas", List.of("pandas"));
        m.put("NumPy", List.of("numpy"));
        m.put("C++", List.of("c++", "cpp"));
        m.put("C#", List.of("c#", "csharp", "c sharp"));
        m.put("Go", List.of("golang", "go lang"));
        m.put("Rust", List.of("rust"));
        m.put("Kotlin", List.of("kotlin"));
        m.put("Swift", List.of("swift"));
        m.put("PHP", List.of("php"));
        m.put("Ruby", List.of("ruby"));
        m.put("Rails", List.of("rails", "ruby on rails"));
        m.put("Angular", List.of("angular", "angularjs"));
        m.put("Vue.js", List.of("vue", "vue.js", "vuejs"));
        m.put("REST API", List.of("rest api", "restful", "rest service", "rest services"));
        m.put("GraphQL", List.of("graphql"));
        m.put("Git", List.of("git", "github", "gitlab", "bitbucket"));
        m.put("Docker", List.of("docker"));
        m.put("Kubernetes", List.of("kubernetes", "k8s"));
        m.put("AWS", List.of("aws", "amazon web services", "ec2", "s3", "lambda"));
        m.put("Azure", List.of("azure", "microsoft azure"));
        m.put("GCP", List.of("gcp", "google cloud"));
        m.put("Linux", List.of("linux", "ubuntu", "debian", "centos"));
        m.put("Jenkins", List.of("jenkins"));
        m.put("CI/CD", List.of("ci/cd", "cicd", "continuous integration"));
        m.put("Terraform", List.of("terraform"));
        m.put("Ansible", List.of("ansible"));
        m.put("Elasticsearch", List.of("elasticsearch", "elastic search"));
        m.put("Kafka", List.of("kafka"));
        m.put("RabbitMQ", List.of("rabbitmq", "rabbit mq"));
        m.put("Microservices", List.of("microservices", "micro service"));
        m.put("Android", List.of("android"));
        m.put("Flutter", List.of("flutter"));
        m.put("React Native", List.of("react native"));
        m.put("TensorFlow", List.of("tensorflow"));
        m.put("PyTorch", List.of("pytorch"));
        m.put("Machine Learning", List.of("machine learning", "deep learning", "neural network"));
        m.put("Data Structures", List.of("data structures", "data structure", "algorithms", "leetcode"));
        m.put("System Design", List.of("system design"));
        m.put("Postman", List.of("postman"));
        m.put("Figma", List.of("figma"));
        m.put("Webpack", List.of("webpack"));
        m.put("Vite", List.of("vite"));
        m.put("npm", List.of("npm", "yarn", "pnpm"));
        return m;
    }

    static String normalizeForSearch(String s) {
        String norm = Normalizer.normalize(s, Normalizer.Form.NFKC);
        norm = norm.replace('\u00A0', ' ');
        norm = norm.toLowerCase(Locale.ROOT);
        norm = norm.replaceAll("[._]+", " ");
        norm = norm.replaceAll("[^a-z0-9+.#\\s]", " ");
        norm = norm.replaceAll("\\s+", " ").trim();
        return norm;
    }
}

