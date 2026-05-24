package com.hirenest.backend.controller;

import com.hirenest.backend.dto.AuthDtos.ForgotPasswordRequest;
import com.hirenest.backend.dto.AuthDtos.ForgotPasswordResponse;
import com.hirenest.backend.dto.AuthDtos.GoogleAuthRequest;
import com.hirenest.backend.dto.AuthDtos.LoginRequest;
import com.hirenest.backend.dto.AuthDtos.LoginResponse;
import com.hirenest.backend.dto.AuthDtos.RegisterRequest;
import com.hirenest.backend.dto.AuthDtos.ResetPasswordRequest;
import com.hirenest.backend.dto.AuthDtos.SimpleMessageResponse;
import com.hirenest.backend.repository.UserRepository;
import com.hirenest.backend.service.AuthService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes all /api/auth/** endpoints.
 * Overrides the base JAR's AuthController (same package + class name,
 * so the Maven compiler replaces the prebuilt .class file).
 *
 * Public paths (no JWT required) — whitelisted in SecurityConfig:
 *   POST /api/auth/register
 *   POST /api/auth/login
 *   POST /api/auth/google
 *   POST /api/auth/forgot-password
 *   POST /api/auth/reset-password
 *   GET  /api/auth/debug-users   ← diagnostic; REMOVE before production release
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        var user = authService.register(request);
        // Return only safe scalar fields — avoids Jackson serialization issues
        // with lazy-loaded JPA relationships on the User entity from the base JAR.
        return ResponseEntity.ok(Map.of(
                "userId",   user.getId()       != null ? user.getId()       : -1L,
                "fullName", user.getFullName() != null ? user.getFullName() : "",
                "email",    user.getEmail()    != null ? user.getEmail()    : "",
                "role",     user.getRole()     != null ? user.getRole()     : ""
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponse> googleAuth(@RequestBody GoogleAuthRequest request) {
        LoginResponse response = authService.googleAuth(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        ForgotPasswordResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<SimpleMessageResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        SimpleMessageResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Diagnostic endpoint — returns all registered emails.
     * Uses a JPQL projection query (SELECT u.email FROM User u) to avoid
     * loading full User entities and triggering lazy-load exceptions.
     * REMOVE or restrict this before going to production.
     *
     * GET /api/auth/debug-users
     */
    @GetMapping("/debug-users")
    public ResponseEntity<List<String>> debugUsers() {
        log.info("[debug-users] endpoint called");
        try {
            List<String> emails = userRepository.findAllEmails();
            log.info("[debug-users] total users found: {}", emails.size());
            emails.forEach(e -> log.info("[debug-users] email: {}", e));
            return ResponseEntity.ok(emails);
        } catch (Exception ex) {
            log.error("[debug-users] failed to query emails", ex);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }
}
