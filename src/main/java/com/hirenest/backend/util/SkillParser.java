package com.hirenest.backend.util;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class SkillParser {
    private SkillParser() {}

    public static Set<String> splitSkills(String csv) {
        Set<String> out = new HashSet<>();
        if (csv == null || csv.isBlank()) {
            return out;
        }
        String[] parts = csv.split("[,;|]+");
        for (String p : parts) {
            String normalized = p.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isBlank()) {
                out.add(normalized);
            }
        }
        return out;
    }
}

