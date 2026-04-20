package com.hirenest.backend.util;

import com.hirenest.backend.dto.JobDtos.LearningRecommendation;
import com.hirenest.backend.dto.JobDtos.LearningResourceDto;
import com.hirenest.backend.dto.JobDtos.LearningRoadmapDto;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LearningResourceMapper {
    private static final Map<String, ResourceSet> SKILL_LINKS = buildSkillLinks();
    /** Maps alternate normalizations (e.g. nodejs â†’ node.js) to canonical SKILL_LINKS keys. */
    private static final Map<String, String> SKILL_ALIASES = buildSkillAliases();

    private LearningResourceMapper() {}

    public static String linkFor(String skill) {
        if (skill == null || skill.isBlank()) {
            return "https://www.google.com/search?q=learn+skills";
        }
        String normalized = skill.trim().toLowerCase(Locale.ROOT);
        String key = resolveCanonicalKey(normalized);
        ResourceSet known = SKILL_LINKS.get(key);
        if (known != null) {
            return known.primaryLink;
        }
        return "https://www.google.com/search?q=learn+" +
                URLEncoder.encode(normalized, StandardCharsets.UTF_8);
    }

    public static LearningRecommendation recommendationFor(String skill) {
        LearningRoadmapDto roadmap = roadmapFor(skill);
        LearningRecommendation rec = new LearningRecommendation();
        rec.skillName = roadmap.skillName;
        rec.beginnerLink = firstUrl(roadmap.beginnerResources);
        rec.intermediateLink = firstUrl(roadmap.intermediateResources);
        rec.advancedLink = firstUrl(roadmap.advancedResources);
        rec.primaryLink = firstNonNullUrl(rec.beginnerLink, rec.intermediateLink, rec.advancedLink);
        rec.resourceType = firstType(roadmap.beginnerResources, roadmap.intermediateResources, roadmap.advancedResources);
        return rec;
    }

    public static LearningRoadmapDto roadmapFor(String skill) {
        String pretty = prettifySkill(skill);
        String normalized = normalizeSkill(skill);
        String canonicalKey = resolveCanonicalKey(normalized);
        ResourceSet set = SKILL_LINKS.get(canonicalKey);

        LearningRoadmapDto roadmap = new LearningRoadmapDto();
        roadmap.skillName = pretty;
        if (set == null) {
            roadmap.beginnerResources = List.of(new LearningResourceDto(
                    "Learn " + pretty + " basics",
                    googleSearch("learn " + pretty + " basics"),
                    "course"
            ));
            roadmap.intermediateResources = List.of(new LearningResourceDto(
                    "Intermediate " + pretty + " projects",
                    googleSearch("intermediate " + pretty + " projects"),
                    "project"
            ));
            roadmap.advancedResources = List.of(new LearningResourceDto(
                    "Advanced " + pretty + " tutorials",
                    googleSearch("advanced " + pretty + " tutorials"),
                    "documentation"
            ));
            return roadmap;
        }

        roadmap.beginnerResources = set.beginnerResources;
        roadmap.intermediateResources = set.intermediateResources;
        roadmap.advancedResources = set.advancedResources;
        return roadmap;
    }

    private static String prettifySkill(String skill) {
        if (skill == null || skill.isBlank()) return "Skill";
        String trimmed = skill.trim();
        if (trimmed.equalsIgnoreCase("sql")) return "SQL";
        if (trimmed.equalsIgnoreCase("html")) return "HTML";
        if (trimmed.equalsIgnoreCase("css")) return "CSS";
        if (trimmed.equalsIgnoreCase("c++")) return "C++";
        if (trimmed.equalsIgnoreCase("node.js")) return "Node.js";
        String[] parts = trimmed.toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isBlank()) continue;
            if (i > 0) out.append(" ");
            out.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return out.toString();
    }

    private static String normalizeSkill(String skill) {
        if (skill == null) return "";
        return skill.trim().toLowerCase(Locale.ROOT);
    }

    /** Resolve user/job skill string to a key present in {@link #SKILL_LINKS}, or return input for fallback roadmaps. */
    private static String resolveCanonicalKey(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return "";
        }
        if (SKILL_LINKS.containsKey(normalized)) {
            return normalized;
        }
        return SKILL_ALIASES.getOrDefault(normalized, normalized);
    }

    private static Map<String, String> buildSkillAliases() {
        Map<String, String> m = new HashMap<>();
        m.put("nodejs", "node.js");
        m.put("node js", "node.js");
        m.put("node", "node.js");
        m.put("springboot", "spring boot");
        m.put("mongo db", "mongodb");
        m.put("mongo", "mongodb");
        m.put("cpp", "c++");
        return m;
    }

    private static Map<String, ResourceSet> buildSkillLinks() {
        Map<String, ResourceSet> links = new HashMap<>();
        links.put("java", ResourceSet.of(
                List.of(r("Java Basics (Official)", "https://dev.java/learn/", "course")),
                List.of(
                        r("Core Java OOP + Collections", "https://www.baeldung.com/java-tutorial", "documentation"),
                        r("Java Problem Solving Practice", "https://www.hackerrank.com/domains/java", "project")
                ),
                List.of(
                        r("Advanced Java Patterns", "https://www.baeldung.com/java-design-patterns", "documentation"),
                        r("Backend System Design with Java", googleSearch("advanced java backend system design project"), "project")
                )
        ));
        links.put("spring boot", ResourceSet.of(
                List.of(r("Spring Boot for Beginners", "https://spring.io/guides/gs/spring-boot", "documentation")),
                List.of(
                        r("Build REST API with Spring Boot", "https://spring.io/guides/gs/rest-service/", "project"),
                        r("Spring Boot + JPA", "https://www.baeldung.com/the-persistence-layer-with-spring-data-jpa", "documentation")
                ),
                List.of(
                        r("Spring Boot Reference Docs", "https://docs.spring.io/spring-boot/index.html", "documentation"),
                        r("Advanced Spring Boot Projects", googleSearch("advanced spring boot project architecture"), "project")
                )
        ));
        links.put("mysql", ResourceSet.of(
                List.of(r("MySQL Basics", "https://www.w3schools.com/mysql/", "course")),
                List.of(
                        r("MySQL Tutorial (Intermediate)", "https://www.geeksforgeeks.org/mysql-tutorial/", "documentation"),
                        r("SQL Joins and Optimization Practice", googleSearch("mysql joins indexing practice"), "project")
                ),
                List.of(
                        r("MySQL Official Documentation", "https://dev.mysql.com/doc/", "documentation"),
                        r("Database Performance Tuning", googleSearch("mysql performance tuning advanced"), "documentation")
                )
        ));
        links.put("react", ResourceSet.of(
                List.of(r("React Learn Basics", "https://react.dev/learn", "course")),
                List.of(
                        r("Hooks + State Management", "https://www.freecodecamp.org/news/learn-react-course/", "video"),
                        r("Intermediate React Project", googleSearch("react intermediate dashboard project"), "project")
                ),
                List.of(
                        r("React Reference", "https://react.dev/reference/react", "documentation"),
                        r("React Performance Optimization", googleSearch("react performance optimization guide"), "documentation")
                )
        ));
        links.put("javascript", ResourceSet.of(
                List.of(r("JavaScript First Steps", "https://developer.mozilla.org/en-US/docs/Learn/JavaScript/First_steps", "documentation")),
                List.of(
                        r("Modern JavaScript Guide", "https://javascript.info/", "documentation"),
                        r("JavaScript Projects", googleSearch("javascript intermediate projects"), "project")
                ),
                List.of(
                        r("Advanced JavaScript Patterns", "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide", "documentation"),
                        r("Async/Performance Deep Dive", googleSearch("advanced javascript async performance"), "documentation")
                )
        ));
        links.put("python", ResourceSet.of(
                List.of(r("Python Basics", "https://www.learnpython.org/", "course")),
                List.of(
                        r("Intermediate Python", "https://realpython.com/", "documentation"),
                        r("Python Project Practice", googleSearch("python intermediate project ideas"), "project")
                ),
                List.of(
                        r("Python Official Tutorial", "https://docs.python.org/3/tutorial/", "documentation"),
                        r("Advanced Python Concepts", googleSearch("advanced python concepts"), "documentation")
                )
        ));
        links.put("html", ResourceSet.of(
                List.of(r("HTML Learning Path", "https://developer.mozilla.org/en-US/docs/Learn/HTML", "documentation")),
                List.of(
                        r("Responsive Web Design", "https://www.freecodecamp.org/learn/2022/responsive-web-design/", "course"),
                        r("HTML Portfolio Project", googleSearch("html portfolio project tutorial"), "project")
                ),
                List.of(
                        r("HTML Reference", "https://developer.mozilla.org/en-US/docs/Web/HTML", "documentation"),
                        r("Semantic HTML Best Practices", googleSearch("advanced semantic html best practices"), "documentation")
                )
        ));
        links.put("css", ResourceSet.of(
                List.of(r("CSS Learning Path", "https://developer.mozilla.org/en-US/docs/Learn/CSS", "documentation")),
                List.of(
                        r("Responsive Design Practice", "https://www.freecodecamp.org/learn/2022/responsive-web-design/", "course"),
                        r("CSS Layout Projects", googleSearch("css flexbox grid project practice"), "project")
                ),
                List.of(
                        r("CSS Reference", "https://developer.mozilla.org/en-US/docs/Web/CSS", "documentation"),
                        r("Advanced CSS Animations", googleSearch("advanced css animations guide"), "video")
                )
        ));
        links.put("rest api", ResourceSet.of(
                List.of(r("REST API Basics", "https://restfulapi.net/", "documentation")),
                List.of(
                        r("Build REST API Project", googleSearch("build rest api project tutorial"), "project"),
                        r("Postman API Testing", googleSearch("postman rest api testing tutorial"), "video")
                ),
                List.of(
                        r("REST API Best Practices", googleSearch("rest api best practices advanced"), "documentation"),
                        r("API Security Guide", googleSearch("rest api security oauth jwt"), "documentation")
                )
        ));
        links.put("git", ResourceSet.of(
                List.of(r("Git Getting Started", "https://git-scm.com/book/en/v2/Getting-Started-What-is-Git%3F", "documentation")),
                List.of(
                        r("Learn Git Branching", "https://learngitbranching.js.org/", "project"),
                        r("Git Workflow Practice", googleSearch("git workflow branching strategy"), "project")
                ),
                List.of(
                        r("Git Official Docs", "https://git-scm.com/doc", "documentation"),
                        r("Advanced Git Rebase Strategies", googleSearch("advanced git rebase workflow"), "documentation")
                )
        ));
        links.put("docker", ResourceSet.of(
                List.of(r("Docker Getting Started", "https://docs.docker.com/get-started/", "course")),
                List.of(
                        r("Docker Hands-on Lab", "https://labs.play-with-docker.com/", "project"),
                        r("Containerize Web App", googleSearch("dockerize spring boot app tutorial"), "project")
                ),
                List.of(
                        r("Docker Engine Docs", "https://docs.docker.com/engine/", "documentation"),
                        r("Docker Compose + Deployment", googleSearch("advanced docker compose deployment"), "documentation")
                )
        ));
        links.put("sql", ResourceSet.of(
                List.of(r("SQL Basics", "https://www.w3schools.com/sql/", "course")),
                List.of(
                        r("Intermediate SQL Tutorial", "https://mode.com/sql-tutorial/", "course"),
                        r("SQL Query Challenges", googleSearch("sql intermediate query challenges"), "project")
                ),
                List.of(
                        r("SQL Performance Guide", "https://use-the-index-luke.com/", "documentation"),
                        r("Advanced SQL Optimization", googleSearch("advanced sql optimization"), "documentation")
                )
        ));
        links.put("node.js", ResourceSet.of(
                List.of(r("Node.js Getting Started", "https://nodejs.org/en/learn/getting-started/introduction-to-nodejs", "documentation")),
                List.of(
                        r("Express.js Guide", "https://expressjs.com/en/starter/installing.html", "documentation"),
                        r("REST API with Node", googleSearch("node.js express rest api tutorial"), "project")
                ),
                List.of(
                        r("Node.js Best Practices", "https://github.com/goldbergyoni/nodebestpractices", "documentation"),
                        r("Advanced Node.js Performance", googleSearch("advanced node.js performance clustering"), "documentation")
                )
        ));
        links.put("mongodb", ResourceSet.of(
                List.of(r("MongoDB Basics", "https://www.mongodb.com/docs/manual/introduction/", "documentation")),
                List.of(
                        r("Mongoose ODM", "https://mongoosejs.com/docs/guide.html", "documentation"),
                        r("MongoDB Aggregation Practice", googleSearch("mongodb aggregation pipeline tutorial"), "project")
                ),
                List.of(
                        r("MongoDB Atlas + Scaling", "https://www.mongodb.com/docs/atlas/", "documentation"),
                        r("Advanced MongoDB Indexing", googleSearch("advanced mongodb indexing sharding"), "documentation")
                )
        ));
        links.put("aws", ResourceSet.of(
                List.of(r("AWS Getting Started", "https://aws.amazon.com/getting-started/", "course")),
                List.of(
                        r("AWS Core Services Overview", "https://docs.aws.amazon.com/whitepapers/latest/aws-overview/introduction.html", "documentation"),
                        r("Deploy a Web App on AWS", googleSearch("aws deploy web app ec2 s3 tutorial"), "project")
                ),
                List.of(
                        r("AWS Well-Architected", "https://aws.amazon.com/architecture/well-architected/", "documentation"),
                        r("Advanced AWS Architecture", googleSearch("advanced aws architecture serverless"), "documentation")
                )
        ));
        links.put("bootstrap", ResourceSet.of(
                List.of(r("Bootstrap Docs â€” Get Started", "https://getbootstrap.com/docs/5.3/getting-started/introduction/", "documentation")),
                List.of(
                        r("Bootstrap Layout + Components", "https://getbootstrap.com/docs/5.3/layout/grid/", "documentation"),
                        r("Responsive Page Project", googleSearch("bootstrap 5 responsive landing page project"), "project")
                ),
                List.of(
                        r("Bootstrap Customization", "https://getbootstrap.com/docs/5.3/customize/overview/", "documentation"),
                        r("Advanced Sass + Bootstrap", googleSearch("advanced bootstrap sass customization"), "documentation")
                )
        ));
        links.put("c++", ResourceSet.of(
                List.of(r("Learn C++ Basics", "https://www.learncpp.com/", "course")),
                List.of(
                        r("C++ STL & Algorithms", "https://www.geeksforgeeks.org/c-plus-plus/", "documentation"),
                        r("C++ Project Practice", googleSearch("c++ intermediate oop project"), "project")
                ),
                List.of(
                        r("Modern C++ Features", "https://en.cppreference.com/w/cpp/language", "documentation"),
                        r("Advanced C++ Design", googleSearch("advanced c++ design patterns"), "documentation")
                )
        ));
        links.put("c", ResourceSet.of(
                List.of(r("C Programming â€” Basics", "https://www.programiz.com/c-programming", "course")),
                List.of(
                        r("Pointers & Memory in C", "https://www.cs.yale.edu/homes/aspnes/classes/223/notes.html", "documentation"),
                        r("C Mini Projects", googleSearch("c programming intermediate projects"), "project")
                ),
                List.of(
                        r("C Standard Library Reference", "https://en.cppreference.com/w/c", "documentation"),
                        r("Advanced C Systems Programming", googleSearch("advanced c systems programming"), "documentation")
                )
        ));
        return links;
    }

    private static LearningResourceDto r(String title, String url, String type) {
        return new LearningResourceDto(title, url, type);
    }

    private static String googleSearch(String query) {
        return "https://www.google.com/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
    }

    private static String firstUrl(List<LearningResourceDto> items) {
        if (items == null || items.isEmpty()) return null;
        return items.get(0).url;
    }

    private static String firstNonNullUrl(String... urls) {
        if (urls == null) return null;
        for (String u : urls) {
            if (u != null && !u.isBlank()) return u;
        }
        return null;
    }

    @SafeVarargs
    private static String firstType(List<LearningResourceDto>... lists) {
        for (List<LearningResourceDto> items : lists) {
            if (items != null && !items.isEmpty() && items.get(0).type != null) return items.get(0).type;
        }
        return "course";
    }

    private static final class ResourceSet {
        private final List<LearningResourceDto> beginnerResources;
        private final List<LearningResourceDto> intermediateResources;
        private final List<LearningResourceDto> advancedResources;
        private final String primaryLink;

        private ResourceSet(List<LearningResourceDto> beginnerResources,
                            List<LearningResourceDto> intermediateResources,
                            List<LearningResourceDto> advancedResources) {
            this.beginnerResources = copy(beginnerResources);
            this.intermediateResources = copy(intermediateResources);
            this.advancedResources = copy(advancedResources);
            this.primaryLink = firstUrl(this.beginnerResources) != null
                    ? firstUrl(this.beginnerResources)
                    : firstUrl(this.intermediateResources);
        }

        private static ResourceSet of(List<LearningResourceDto> beginnerResources,
                                      List<LearningResourceDto> intermediateResources,
                                      List<LearningResourceDto> advancedResources) {
            return new ResourceSet(beginnerResources, intermediateResources, advancedResources);
        }

        private List<LearningResourceDto> copy(List<LearningResourceDto> in) {
            if (in == null) return List.of();
            return new ArrayList<>(in);
        }
    }
}

