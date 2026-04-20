package com.hirenest.backend.mapper;

import com.hirenest.backend.dto.ApplicationDtos;
import com.hirenest.backend.entity.JobApplication;

public final class ApplicationMapper {
    private ApplicationMapper() {}

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

        if (a.getJob() != null) {
            ApplicationDtos.JobMini j = new ApplicationDtos.JobMini();
            j.id = a.getJob().getId();
            j.title = a.getJob().getTitle();
            j.companyName = a.getJob().getCompanyName();
            j.location = a.getJob().getLocation();
            j.remote = a.getJob().getRemote();
            r.job = j;
        }
        if (a.getUser() != null) {
            ApplicationDtos.UserMini u = new ApplicationDtos.UserMini();
            u.id = a.getUser().getId();
            u.fullName = a.getUser().getFullName();
            r.user = u;
        }
        return r;
    }
}

