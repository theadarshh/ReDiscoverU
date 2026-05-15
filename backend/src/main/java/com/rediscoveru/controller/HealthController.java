package com.rediscoveru.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check endpoint — used by frontend to verify backend is reachable.
 * GET /api/health → 200 OK
 */
@RestController
@CrossOrigin(origins = "*")
public class HealthController {

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",    "UP");
        body.put("service",   "ReDiscoverU");
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("message",   "Backend is running");
        return ResponseEntity.ok(body);
    }
}
