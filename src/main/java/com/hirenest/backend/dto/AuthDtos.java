package com.hirenest.backend.dto;

public class AuthDtos {
    public static class RegisterRequest {
        public String fullName;
        public String email;
        public String password;
        public String role;
    }

    public static class LoginRequest {
        public String email;
        public String password;
    }

    public static class LoginResponse {
        public Long userId;
        public String fullName;
        public String email;
        public String role;
        public Long recruiterId;
        public String token;
    }

    public static class GoogleAuthRequest {
        public String idToken;
        public String credential;
        public String role;
    }

    public static class ForgotPasswordRequest {
        public String email;
    }

    public static class ForgotPasswordResponse {
        public String message;
        public String resetToken; // Demo-friendly; replace with email delivery in production.
        public long expiresInSeconds;
    }

    public static class ResetPasswordRequest {
        public String token;
        public String newPassword;
        public String confirmPassword;
    }

    public static class SimpleMessageResponse {
        public String message;
    }
}

