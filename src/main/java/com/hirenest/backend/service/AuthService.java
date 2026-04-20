package com.hirenest.backend.service;

import com.hirenest.backend.dto.AuthDtos.LoginRequest;
import com.hirenest.backend.dto.AuthDtos.LoginResponse;
import com.hirenest.backend.dto.AuthDtos.ForgotPasswordRequest;
import com.hirenest.backend.dto.AuthDtos.ForgotPasswordResponse;
import com.hirenest.backend.dto.AuthDtos.GoogleAuthRequest;
import com.hirenest.backend.dto.AuthDtos.RegisterRequest;
import com.hirenest.backend.dto.AuthDtos.ResetPasswordRequest;
import com.hirenest.backend.dto.AuthDtos.SimpleMessageResponse;
import com.hirenest.backend.entity.PasswordResetToken;
import com.hirenest.backend.entity.RecruiterProfile;
import com.hirenest.backend.entity.User;
import com.hirenest.backend.exception.BadRequestException;
import com.hirenest.backend.exception.NotFoundException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.hirenest.backend.repository.PasswordResetTokenRepository;
import com.hirenest.backend.repository.RecruiterProfileRepository;
import com.hirenest.backend.repository.UserRepository;
import com.hirenest.backend.security.JwtUtil;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Set<String> ALLOWED_ROLES = Set.of("JOB_SEEKER", "RECRUITER");
    private final UserRepository userRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private static final long RESET_TOKEN_EXPIRY_MINUTES = 20;

    public AuthService(UserRepository userRepository, RecruiterProfileRepository recruiterProfileRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       @Value("${google.client.id:${app.google.client-id:}}") String googleClientId) {
        this.userRepository = userRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        GoogleIdTokenVerifier.Builder verifierBuilder = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        );
        if (googleClientId != null && !googleClientId.isBlank()) {
            verifierBuilder.setAudience(Collections.singletonList(googleClientId.trim()));
        }
        this.googleIdTokenVerifier = verifierBuilder.build();
    }

    public User register(RegisterRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        if (request.fullName == null || request.fullName.trim().isEmpty()) {
            throw new BadRequestException("Full name is required");
        }
        if (request.email == null || request.email.trim().isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        if (request.password == null || request.password.trim().isEmpty()) {
            throw new BadRequestException("Password is required");
        }
        String normalizedEmail = request.email.trim().toLowerCase(Locale.ROOT);
        String normalizedRole = request.role == null ? "JOB_SEEKER" : request.role.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            throw new BadRequestException("Invalid role value. Allowed values: JOB_SEEKER, RECRUITER");
        }

        userRepository.findByEmail(normalizedEmail).ifPresent(u -> {
            throw new BadRequestException("Email already exists");
        });
        User user = new User();
        user.setFullName(request.fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.password));
        user.setRole(normalizedRole);
        user.setAuthProvider("LOCAL");
        user.setProviderId(null);
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException("Database constraint failed while creating user");
        }
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email)
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password, user.getPassword())) {
            throw new BadRequestException("Invalid credentials");
        }
        return buildLoginResponse(user);
    }

    public LoginResponse googleAuth(GoogleAuthRequest request) {
        String rawToken = request == null ? null : request.idToken;
        if ((rawToken == null || rawToken.trim().isEmpty()) && request != null) {
            rawToken = request.credential;
        }
        if (rawToken == null || rawToken.trim().isEmpty()) {
            throw new BadRequestException("Google credential is required");
        }
        GoogleIdToken idToken;
        try {
            idToken = googleIdTokenVerifier.verify(rawToken.trim());
        } catch (Exception ex) {
            log.error("Google token verification failed", ex);
            throw new BadRequestException("Failed to verify Google token");
        }
        if (idToken == null || idToken.getPayload() == null) {
            throw new BadRequestException("Invalid Google token");
        }
        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String fullName = (String) payload.get("name");
        String providerId = payload.getSubject();
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Google account email is missing");
        }
        if (providerId == null || providerId.isBlank()) {
            throw new BadRequestException("Google account identifier is missing");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedRole = normalizeRequestedRole(request == null ? null : request.role);
        User user = userRepository.findByProviderId(providerId)
                .or(() -> userRepository.findByEmail(normalizedEmail))
                .orElseGet(() -> createGoogleUser(normalizedEmail, fullName, providerId, normalizedRole));

        // Link provider ID to existing account with same verified Google email.
        if (user.getProviderId() == null || user.getProviderId().isBlank()) {
            user.setProviderId(providerId);
            if (user.getAuthProvider() == null || user.getAuthProvider().isBlank()) {
                user.setAuthProvider("LOCAL");
            }
            user = userRepository.save(user);
        }

        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole(normalizedRole);
            user = userRepository.save(user);
        }

        return buildLoginResponse(user);
    }

    private String normalizeRequestedRole(String requestedRole) {
        String normalizedRole = requestedRole == null ? "JOB_SEEKER" : requestedRole.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            throw new BadRequestException("Invalid role value. Allowed values: JOB_SEEKER, RECRUITER");
        }
        return normalizedRole;
    }

    private User createGoogleUser(String email, String fullName, String providerId, String role) {
        User user = new User();
        user.setEmail(email);
        user.setFullName((fullName == null || fullName.isBlank()) ? "Google User" : fullName.trim());
        user.setRole(role);
        user.setAuthProvider("GOOGLE");
        user.setProviderId(providerId);
        // Password remains required in schema; generate an internal random value for Google-only accounts.
        user.setPassword(passwordEncoder.encode("GOOGLE_" + UUID.randomUUID()));
        return userRepository.save(user);
    }

    private LoginResponse buildLoginResponse(User user) {
        LoginResponse response = new LoginResponse();
        response.userId = user.getId();
        response.fullName = user.getFullName();
        response.email = user.getEmail();
        response.role = user.getRole();
        response.token = jwtUtil.generateToken(user.getId(), user.getRole());
        List<RecruiterProfile> rpRows = recruiterProfileRepository.findAllByUserIdOrderByIdAsc(user.getId());
        if (!rpRows.isEmpty()) {
            response.recruiterId = rpRows.get(0).getId();
        }
        return response;
    }

    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        if (request == null || request.email == null || request.email.isBlank()) {
            throw new BadRequestException("Email is required");
        }
        User user = userRepository.findByEmail(request.email.trim())
                .orElseThrow(() -> new NotFoundException("User not found with this email"));

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiryTime(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES));
        token.setUsed(false);
        passwordResetTokenRepository.save(token);

        ForgotPasswordResponse response = new ForgotPasswordResponse();
        response.message = "Reset token generated. Use it to reset your password.";
        response.resetToken = token.getToken(); // demo/testing path
        response.expiresInSeconds = RESET_TOKEN_EXPIRY_MINUTES * 60L;
        return response;
    }

    public SimpleMessageResponse resetPassword(ResetPasswordRequest request) {
        if (request == null || request.token == null || request.token.isBlank()) {
            throw new BadRequestException("Reset token is required");
        }
        if (request.newPassword == null || request.newPassword.isBlank()) {
            throw new BadRequestException("New password is required");
        }
        if (request.confirmPassword == null || request.confirmPassword.isBlank()) {
            throw new BadRequestException("Confirm password is required");
        }
        if (!request.newPassword.equals(request.confirmPassword)) {
            throw new BadRequestException("Passwords do not match");
        }
        if (request.newPassword.length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters");
        }

        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.token.trim())
                .orElseThrow(() -> new BadRequestException("Invalid reset token"));
        if (Boolean.TRUE.equals(token.getUsed())) {
            throw new BadRequestException("Reset token already used");
        }
        if (token.getExpiryTime() == null || token.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset token expired");
        }
        User user = token.getUser();
        if (user == null) {
            throw new BadRequestException("Reset token is not associated with a user");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword));
        userRepository.save(user);

        token.setUsed(true);
        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);

        SimpleMessageResponse response = new SimpleMessageResponse();
        response.message = "Password reset successful. Please login with your new password.";
        return response;
    }
}

