package com.rediscoveru.controller;

import com.rediscoveru.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Gamification Controller
 * Base path: /api/gamification
 *
 * Streak tracking, badge system, achievements, leaderboard.
 */
@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamifService;

    /** GET /api/gamification/streak — user's current streak */
    @GetMapping("/streak")
    public ResponseEntity<?> getStreak(@AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(gamifService.getStreak(ud.getUsername())); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** GET /api/gamification/badges — all badges the user has earned */
    @GetMapping("/badges")
    public ResponseEntity<?> getBadges(@AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(gamifService.getUserBadges(ud.getUsername())); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** GET /api/gamification/badges/all — all available badges (earned + unearned) */
    @GetMapping("/badges/all")
    public ResponseEntity<?> allBadges(@AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(gamifService.getAllBadges()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** GET /api/gamification/achievements — full achievement summary */
    @GetMapping("/achievements")
    public ResponseEntity<?> achievements(@AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(gamifService.getAchievements(ud.getUsername())); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** GET /api/gamification/leaderboard — top 10 streak leaders */
    @GetMapping("/leaderboard")
    public ResponseEntity<?> leaderboard(@AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(gamifService.getLeaderboard()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** POST /api/gamification/activity — record a learning activity (updates streak) */
    @PostMapping("/activity")
    public ResponseEntity<?> recordActivity(@AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(gamifService.recordActivity(ud.getUsername())); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
}
