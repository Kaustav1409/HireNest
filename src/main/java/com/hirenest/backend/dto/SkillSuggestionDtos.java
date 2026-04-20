package com.hirenest.backend.dto;

import java.util.List;
import java.util.Map;

public class SkillSuggestionDtos {
    public static class NamedLink {
        public String label;
        public String url;

        public NamedLink() {}

        public NamedLink(String label, String url) {
            this.label = label;
            this.url = url;
        }
    }

    public static class SkillSuggestion {
        public String skill;
        public String importance;
        /** Flat links (legacy / quick actions): youtube, course, docs. */
        public Map<String, String> learningLinks;
        /** Grouped paths: Beginner, Intermediate, Advanced â†’ list of labeled URLs. */
        public Map<String, List<NamedLink>> linksByLevel;
    }

    public static class SkillSuggestionResponse {
        public List<SkillSuggestion> suggestions;
    }
}


