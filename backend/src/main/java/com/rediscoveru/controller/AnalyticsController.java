package com.rediscoveru.controller;

import com.rediscoveru.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin Analytics Controller
 * Base path: /api/admin/analytics
 *
 * All endpoints require ROLE_ADMIN (enforced by SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /** GET /api/admin/analytics/overview — top-level platform stats */
    @GetMapping("/overview")
    public ResponseEntity<?> overview() {
        try { return ResponseEntity.ok(analyticsService.getOverviewStats()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** GET /api/admin/analytics/users — user growth and conversion stats */
    @GetMapping("/users")
    public ResponseEntity<?> userStats() {
        try { return ResponseEntity.ok(analyticsService.getUserStats()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** GET /api/admin/analytics/revenue — revenue by month */
    @GetMapping("/revenue")
    public ResponseEntity<?> revenueStats() {
        try { return ResponseEntity.ok(analyticsService.getRevenueStats()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** GET /api/admin/analytics/programs — program engagement stats */
    @GetMapping("/programs")
    public ResponseEntity<?> programEngagement() {
        try { return ResponseEntity.ok(analyticsService.getProgramEngagement()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** GET /api/admin/analytics/top-learners — top 10 most active users */
    @GetMapping("/top-learners")
    public ResponseEntity<?> topLearners() {
        try { return ResponseEntity.ok(analyticsService.getTopLearners()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** GET /api/admin/analytics/activity — recent payment and activity feed */
    @GetMapping("/activity")
    public ResponseEntity<?> activityFeed() {
        try { return ResponseEntity.ok(analyticsService.getActivityFeed()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
}
