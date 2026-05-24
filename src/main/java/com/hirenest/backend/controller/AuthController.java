package com.hirenest.backend.controller;

import com.hirenest.backend.dto.AuthDtos.ForgotPasswordRequest;
import com.hirenest.backend.dto.AuthDtos.ForgotPasswordResponse;
import com.hirenest.backend.dto.AuthDtos.GoogleAuthRequest;
import com.hirenest.backend.dto.AuthDtos.LoginRequest;
import com.hirenest.backend.dto.AuthDtos.LoginResponse;
import com.hirenest.backend.dto.AuthDtos.RegisterRequest;
import com.hirenest.backend.dto.AuthDtos.ResetPasswordRequest;
import com.hirenest.backend.dto.AuthDtos.SimpleMessageResponse;
import com.hirenest.backend.entity.User;
import com.hirenest.backend.repository.UserRepository;
import com.hirenest.backend.service.AuthService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes all /api/auth/** endpoints.
 * Overrides the base JAR's AuthController (same package + class name).
 *
 * Public paths (no JWT required) — whitelisted in SecurityConfig:
 *   POST /api/auth/register
 *   POST /api/auth/login
 *   POST /api/auth/google
 *   POST /api/auth/forgot-password
 *   POST /api/auth/reset-password
 *   GET  /api/auth/debug-users   ← diagnostic; remove before production release
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ResponseEntity.ok(user);
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
     * Diagnostic endpoint — returns all registered emails to verify user records.
     * REMOVE or restrict this before going to production.
     *
     * GET /api/auth/debug-users
     */
    @GetMapping("/debug-users")
    public ResponseEntity<List<String>> debugUsers() {
        List<String> emails = userRepository.findAll()
                .stream()
                .map(User::getEmail)
                .collect(Collectors.toList());
        return ResponseEntity.ok(emails);
    }
}
