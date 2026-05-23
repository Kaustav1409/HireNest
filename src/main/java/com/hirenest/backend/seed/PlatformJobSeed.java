package com.hirenest.backend.seed;

public class PlatformJobSeed {

    public final String sourceKey;
    public final String title;
    public final String companyName;
    public final String description;
    public final String requiredSkills;
    public final String location;
    public final boolean remote;
    public final double salaryMin;
    public final double salaryMax;
    public final String experienceLevel;
    public final String category;
    public final String roleType;

    public PlatformJobSeed(
            String sourceKey,
            String title,
            String companyName,
            String description,
            String requiredSkills,
            String location,
            boolean remote,
            double salaryMin,
            double salaryMax,
            String experienceLevel,
            String category,
            String roleType) {
        this.sourceKey = sourceKey;
        this.title = title;
        this.companyName = companyName;
        this.description = description;
        this.requiredSkills = requiredSkills;
        this.location = location;
        this.remote = remote;
        this.salaryMin = salaryMin;
        this.salaryMax = salaryMax;
        this.experienceLevel = experienceLevel;
        this.category = category;
        this.roleType = roleType;
    }
}
