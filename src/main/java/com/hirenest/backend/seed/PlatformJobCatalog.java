package com.hirenest.backend.seed;

import java.util.ArrayList;
import java.util.List;

/** Builds 175+ platform partner jobs for HireNest ecosystem seeding. */
public final class PlatformJobCatalog {

    public static final String PLATFORM_USER_EMAIL = "platform-network@hirenest.internal";

    private PlatformJobCatalog() {
    }

    public static List<PlatformJobSeed> build() {
        String[] companies = {
                "Amazon", "Flipkart", "TCS", "Infosys", "Wipro", "Google", "Microsoft", "Deloitte",
                "Capgemini", "HCL", "Accenture", "Tech Mahindra", "Swiggy", "Zomato", "Adobe", "Paytm",
                "PhonePe", "Reliance Jio", "Razorpay", "Freshworks", "Zoho", "CRED", "Meesho", "Ola"
        };
        String[] locations = {
                "Bangalore", "Hyderabad", "Pune", "Mumbai", "Chennai", "Delhi NCR", "Kolkata", "Remote India"
        };
        RoleTemplate[] templates = {
                new RoleTemplate("Software Engineer", "Engineering", "Full-time", "java,spring boot,sql,git,data structures",
                        "Build scalable services and APIs for high-traffic products.", 600000, 1800000, "Mid"),
                new RoleTemplate("Frontend Developer", "Engineering", "Full-time", "javascript,react,html,css,git",
                        "Develop responsive UI components and improve web performance.", 500000, 1400000, "Mid"),
                new RoleTemplate("Backend Developer", "Engineering", "Full-time", "java,python,spring boot,rest apis,sql",
                        "Design backend systems, databases, and integration layers.", 650000, 1900000, "Mid"),
                new RoleTemplate("Full Stack Developer", "Engineering", "Full-time", "javascript,react,node.js,sql,mongodb",
                        "Own features end-to-end across frontend and backend stacks.", 700000, 2000000, "Mid"),
                new RoleTemplate("Data Analyst", "Data", "Full-time", "sql,excel,python,power bi,statistics",
                        "Analyze business metrics and deliver actionable dashboards.", 450000, 1200000, "Entry"),
                new RoleTemplate("DevOps Engineer", "Engineering", "Full-time", "docker,kubernetes,aws,linux,ci/cd",
                        "Automate deployments and maintain reliable cloud infrastructure.", 800000, 2200000, "Senior"),
                new RoleTemplate("QA Engineer", "Engineering", "Full-time", "manual testing,selenium,api testing,sql",
                        "Ensure product quality through test planning and automation.", 400000, 1100000, "Entry"),
                new RoleTemplate("UI/UX Designer", "Design", "Full-time", "figma,ui principles,ux principles,wireframing",
                        "Create user-centered designs and interactive prototypes.", 450000, 1300000, "Mid"),
                new RoleTemplate("Machine Learning Engineer", "Data", "Full-time", "python,machine learning basics,sql,statistics",
                        "Train models and deploy ML features for product teams.", 900000, 2500000, "Senior"),
                new RoleTemplate("Cloud Engineer", "Engineering", "Full-time", "aws,cloud basics,linux,docker,terraform",
                        "Operate cloud workloads with security and cost efficiency.", 750000, 2100000, "Senior"),
                new RoleTemplate("Android Developer", "Engineering", "Full-time", "kotlin,java,android,rest apis,git",
                        "Ship mobile features with strong performance and UX.", 550000, 1600000, "Mid"),
                new RoleTemplate("Product Analyst", "Product", "Full-time", "sql,excel,product analytics,communication",
                        "Drive product decisions using data and user research insights.", 500000, 1400000, "Mid"),
                new RoleTemplate("Digital Marketing Specialist", "Marketing", "Full-time", "seo,content marketing,analytics,canva",
                        "Plan campaigns and optimize growth funnels across channels.", 350000, 900000, "Entry"),
                new RoleTemplate("Graphic Designer", "Design", "Full-time", "photoshop,illustrator,branding,layout design",
                        "Produce brand assets and marketing creatives.", 300000, 850000, "Entry"),
                new RoleTemplate("Business Analyst", "Product", "Full-time", "sql,excel,requirements gathering,communication",
                        "Bridge business stakeholders and engineering delivery teams.", 500000, 1300000, "Mid")
        };

        List<PlatformJobSeed> seeds = new ArrayList<>();
        int target = 175;
        int idx = 0;
        for (String company : companies) {
            for (RoleTemplate t : templates) {
                if (seeds.size() >= target) {
                    break;
                }
                String loc = locations[idx % locations.length];
                boolean remote = loc.toLowerCase().contains("remote") || (idx % 4 == 0);
                String key = slug("platform-" + company + "-" + t.title + "-" + loc + "-" + (idx % 3));
                String desc = t.descriptionBase + " Join " + company + " through the HireNest Partner Network. "
                        + "Role focuses on " + t.category.toLowerCase() + " delivery with " + t.experienceLevel
                        + "-level expectations.";
                double min = t.salaryMin * (0.9 + (idx % 5) * 0.02);
                double max = t.salaryMax * (0.95 + (idx % 4) * 0.03);
                seeds.add(new PlatformJobSeed(
                        key,
                        t.title,
                        company,
                        desc,
                        t.skills,
                        loc,
                        remote,
                        min,
                        max,
                        t.experienceLevel,
                        t.category,
                        t.roleType));
                idx++;
            }
            if (seeds.size() >= target) {
                break;
            }
        }
        while (seeds.size() < target) {
            int i = seeds.size();
            String company = companies[i % companies.length];
            RoleTemplate t = templates[i % templates.length];
            String loc = locations[i % locations.length];
            seeds.add(new PlatformJobSeed(
                    slug("platform-extra-" + company + "-" + i),
                    t.title + " (Campus)",
                    company,
                    "Campus-oriented " + t.descriptionBase,
                    t.skills,
                    loc,
                    i % 2 == 0,
                    t.salaryMin * 0.8,
                    t.salaryMax * 0.85,
                    "Entry",
                    t.category,
                    "Internship"));
        }
        return seeds;
    }

    private static String slug(String raw) {
        return raw.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private static final class RoleTemplate {
        final String title;
        final String category;
        final String roleType;
        final String skills;
        final String descriptionBase;
        final double salaryMin;
        final double salaryMax;
        final String experienceLevel;

        RoleTemplate(
                String title,
                String category,
                String roleType,
                String skills,
                String descriptionBase,
                double salaryMin,
                double salaryMax,
                String experienceLevel) {
            this.title = title;
            this.category = category;
            this.roleType = roleType;
            this.skills = skills;
            this.descriptionBase = descriptionBase;
            this.salaryMin = salaryMin;
            this.salaryMax = salaryMax;
            this.experienceLevel = experienceLevel;
        }
    }
}
