package com.hirenest.backend.controller;

import com.hirenest.backend.dto.AuthDtos.LoginRequest;
import com.hirenest.backend.dto.AuthDtos.LoginResponse;
import com.hirenest.backend.dto.AuthDtos.ForgotPasswordRequest;
import com.hirenest.backend.dto.AuthDtos.ForgotPasswordResponse;
import com.hirenest.backend.dto.AuthDtos.GoogleAuthRequest;
import com.hirenest.backend.dto.AuthDtos.RegisterRequest;
import com.hirenest.backend.dto.AuthDtos.ResetPasswordRequest;
import com.hirenest.backend.dto.AuthDtos.SimpleMessageResponse;
import com.hirenest.backend.entity.User;
import com.hirenest.backend.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public User register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/google")
    public LoginResponse googleAuth(@RequestBody GoogleAuthRequest request) {
        return authService.googleAuth(request);
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public SimpleMessageResponse resetPassword(@RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }
}

