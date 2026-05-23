package com.hirenest.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight health-check controller.
 * GET /health → {"status":"ok"}
 * GET /       → {"status":"ok","service":"HireNest Backend"}
 *
 * Both paths are whitelisted in SecurityConfig (no JWT required).
 * Render and other platforms can use GET /health to verify the service is alive.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "ok");
        return ResponseEntity.ok(body);
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> root() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("service", "HireNest Backend");
        return ResponseEntity.ok(body);
    }
}
