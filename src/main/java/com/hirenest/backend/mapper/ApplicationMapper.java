package com.hirenest.backend.mapper;

import com.hirenest.backend.dto.ApplicationDtos;
import com.hirenest.backend.entity.Job;
import com.hirenest.backend.entity.JobApplication;

public final class ApplicationMapper {

    private ApplicationMapper() {
    }

    public static ApplicationDtos.JobApplicationResponse toResponse(JobApplication a) {
        if (a == null) {
            return null;
        }
        ApplicationDtos.JobApplicationResponse r = new ApplicationDtos.JobApplicationResponse();
        r.id = a.getId();
        r.status = a.getStatus();
        r.appliedAt = a.getCreatedAt();
        r.shortlistedAt = a.getShortlistedAt();
        r.rejectedAt = a.getRejectedAt();
        r.applicationStrengthPercent = a.getApplicationStrengthPercent();
        r.hiringConfidencePercent = a.getHiringConfidencePercent();
        r.strengthInsights = a.getStrengthInsights();
        r.githubLink = a.getGithubLink();
        r.portfolioLink = a.getPortfolioLink();
        r.driveLink = a.getDriveLink();
        r.projectDescription = a.getProjectDescription();
        String resumePath = a.getResumeSnapshotPath();
        r.resumeOnFile = resumePath != null && !resumePath.isBlank();
        String projectPath = a.getProjectFilePath();
        r.projectFileOnFile = projectPath != null && !projectPath.isBlank();
        if (a.getJob() != null) {
            r.job = toJobMini(a.getJob());
        }
        if (a.getUser() != null) {
            ApplicationDtos.UserMini u = new ApplicationDtos.UserMini();
            u.id = a.getUser().getId();
            u.fullName = a.getUser().getFullName();
            r.user = u;
        }
        return r;
    }

    public static ApplicationDtos.JobMini toJobMini(Job job) {
        if (job == null) {
            return null;
        }
        ApplicationDtos.JobMini j = new ApplicationDtos.JobMini();
        j.id = job.getId();
        j.title = job.getTitle();
        j.companyName = job.getCompanyName();
        j.location = job.getLocation();
        j.remote = job.getRemote();
        j.platformJob = job.getPlatformJob();
        j.postedByLabel = job.getPostedByLabel();
        j.experienceLevel = job.getExperienceLevel();
        j.category = job.getCategory();
        j.roleType = job.getRoleType();
        return j;
    }
}
