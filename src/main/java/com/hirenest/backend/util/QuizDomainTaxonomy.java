package com.hirenest.backend.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Maps each quiz domain slug (same strings as {@code QuizService} catalog / {@code GET /api/quiz/domains})
 * to the set of internal canonical skill ids produced by {@link QuizSkillNormalizer}. Intersection size drives
 * the base recommendation score for that domain.
 */
public final class QuizDomainTaxonomy {

    private static final Map<String, Set<String>> DOMAIN_TO_CANONICAL;

    static {
        Map<String, Set<String>> m = new LinkedHashMap<>();
        m.put(
                "java",
                Set.of(
                        "java",
                        "oop",
                        "collections",
                        "jdbc",
                        "jvm",
                        "kotlin",
                        "maven",
                        "gradle"));
        m.put(
                "python",
                Set.of(
                        "python",
                        "pandas",
                        "numpy",
                        "flask",
                        "django",
                        "fastapi"));
        m.put(
                "javascript",
                Set.of(
                        "javascript",
                        "typescript",
                        "nodejs",
                        "npm",
                        "webpack",
                        "vite",
                        "dom",
                        "json"));
        m.put("react", Set.of("react", "jsx", "redux", "hooks", "nextjs"));
        m.put(
                "spring boot",
                Set.of(
                        "spring_boot",
                        "spring",
                        "rest",
                        "jpa",
                        "hibernate",
                        "microservices",
                        "maven",
                        "gradle",
                        "java"));
        m.put(
                "sql",
                Set.of(
                        "sql",
                        "mysql",
                        "postgresql",
                        "joins",
                        "queries",
                        "indexing"));
        m.put(
                "dbms",
                Set.of(
                        "dbms",
                        "normalization",
                        "acid",
                        "transactions",
                        "er_diagram",
                        "indexing",
                        "mongodb"));
        m.put(
                "data structures",
                Set.of(
                        "dsa",
                        "arrays",
                        "linked_list",
                        "stack",
                        "queue",
                        "tree",
                        "graph",
                        "heap",
                        "hashmap",
                        "complexity",
                        "algorithms"));
        m.put(
                "web development",
                Set.of(
                        "html",
                        "css",
                        "javascript",
                        "typescript",
                        "bootstrap",
                        "tailwind",
                        "sass",
                        "responsive",
                        "dom",
                        "http",
                        "webpack",
                        "vite",
                        "json",
                        "jsx"));
        m.put("aptitude", Set.of("aptitude", "reasoning"));
        DOMAIN_TO_CANONICAL = Collections.unmodifiableMap(m);
    }

    private QuizDomainTaxonomy() {}

    /** Canonical skill ids that increase the base score for this quiz domain slug. */
    public static Set<String> canonicalSkillsForDomain(String domainSlug) {
        if (domainSlug == null || domainSlug.isBlank()) {
            return Set.of();
        }
        String key = domainSlug.trim().toLowerCase(Locale.ROOT);
        return DOMAIN_TO_CANONICAL.getOrDefault(key, Set.of());
    }

    public static boolean isKnownDomain(String domainSlug) {
        if (domainSlug == null || domainSlug.isBlank()) {
            return false;
        }
        return DOMAIN_TO_CANONICAL.containsKey(domainSlug.trim().toLowerCase(Locale.ROOT));
    }
}

