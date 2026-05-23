package com.hirenest.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

@Entity
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Job job;

    private String status;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime shortlistedAt;

    private LocalDateTime rejectedAt;

    @Column(length = 512)
    private String githubLink;

    @Column(length = 512)
    private String portfolioLink;

    @Column(length = 512)
    private String driveLink;

    @Column(columnDefinition = "LONGTEXT")
    private String projectDescription;

    @Column(length = 1000)
    private String interestedRoles;

    @Column(length = 1000)
    private String strongestSkills;

    @Column(columnDefinition = "LONGTEXT")
    private String contributionDescription;

    @Column(length = 256)
    private String availabilityTypes;

    @Column(length = 512)
    private String resumeSnapshotPath;

    @Column(length = 512)
    private String projectFilePath;

    private Double applicationStrengthPercent;

    private Double hiringConfidencePercent;

    @Column(columnDefinition = "LONGTEXT")
    private String strengthInsights;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getShortlistedAt() {
        return shortlistedAt;
    }

    public void setShortlistedAt(LocalDateTime shortlistedAt) {
        this.shortlistedAt = shortlistedAt;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public String getGithubLink() {
        return githubLink;
    }

    public void setGithubLink(String githubLink) {
        this.githubLink = githubLink;
    }

    public String getPortfolioLink() {
        return portfolioLink;
    }

    public void setPortfolioLink(String portfolioLink) {
        this.portfolioLink = portfolioLink;
    }

    public String getDriveLink() {
        return driveLink;
    }

    public void setDriveLink(String driveLink) {
        this.driveLink = driveLink;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }

    public String getInterestedRoles() {
        return interestedRoles;
    }

    public void setInterestedRoles(String interestedRoles) {
        this.interestedRoles = interestedRoles;
    }

    public String getStrongestSkills() {
        return strongestSkills;
    }

    public void setStrongestSkills(String strongestSkills) {
        this.strongestSkills = strongestSkills;
    }

    public String getContributionDescription() {
        return contributionDescription;
    }

    public void setContributionDescription(String contributionDescription) {
        this.contributionDescription = contributionDescription;
    }

    public String getAvailabilityTypes() {
        return availabilityTypes;
    }

    public void setAvailabilityTypes(String availabilityTypes) {
        this.availabilityTypes = availabilityTypes;
    }

    public String getResumeSnapshotPath() {
        return resumeSnapshotPath;
    }

    public void setResumeSnapshotPath(String resumeSnapshotPath) {
        this.resumeSnapshotPath = resumeSnapshotPath;
    }

    public String getProjectFilePath() {
        return projectFilePath;
    }

    public void setProjectFilePath(String projectFilePath) {
        this.projectFilePath = projectFilePath;
    }

    public Double getApplicationStrengthPercent() {
        return applicationStrengthPercent;
    }

    public void setApplicationStrengthPercent(Double applicationStrengthPercent) {
        this.applicationStrengthPercent = applicationStrengthPercent;
    }

    public Double getHiringConfidencePercent() {
        return hiringConfidencePercent;
    }

    public void setHiringConfidencePercent(Double hiringConfidencePercent) {
        this.hiringConfidencePercent = hiringConfidencePercent;
    }

    public String getStrengthInsights() {
        return strengthInsights;
    }

    public void setStrengthInsights(String strengthInsights) {
        this.strengthInsights = strengthInsights;
    }
}
