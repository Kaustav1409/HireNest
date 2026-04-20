package com.hirenest.backend.dto;

import java.time.LocalDateTime;

public class ApplicationDtos {
    public static class JobMini {
        public Long id;
        public String title;
        public String companyName;
        public String location;
        public Boolean remote;
    }

    public static class UserMini {
        public Long id;
        public String fullName;
    }

    public static class JobApplicationResponse {
        public Long id;
        public String status;
        public LocalDateTime appliedAt;
        public LocalDateTime shortlistedAt;
        public LocalDateTime rejectedAt;
        public JobMini job;
        public UserMini user;
    }
}

