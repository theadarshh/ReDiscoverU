package com.rediscoveru.controller;

import com.rediscoveru.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Personalized Dashboard Controller
 *
 * GET /api/dashboard/home — single call for the entire personalized dashboard.
 * The frontend makes one request on load and gets everything it needs.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/home")
    public ResponseEntity<?> home(@AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(dashboardService.buildDashboard(ud.getUsername())); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
}
