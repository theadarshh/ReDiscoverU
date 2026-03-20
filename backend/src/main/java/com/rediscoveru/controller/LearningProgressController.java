package com.rediscoveru.controller;

import com.rediscoveru.dto.LessonProgressRequest;
import com.rediscoveru.service.LearningProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Learning Progress Controller
 *
 * All endpoints require JWT. User must have PAID subscription (enforced by
 * ProgramController — these endpoints are complementary).
 *
 * Base path: /api/progress
 */
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class LearningProgressController {

    private final LearningProgressService progressService;

    /**
     * POST /api/progress
     * Record or update progress for a single lesson.
     * Called automatically on video timeupdate events or manually on "Mark Complete".
     */
    @PostMapping
    public ResponseEntity<?> recordProgress(
            @RequestBody LessonProgressRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        try {
            return ResponseEntity.ok(progressService.recordProgress(ud.getUsername(), req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/progress/program/{programId}
     * All lesson progress records for a specific program.
     */
    @GetMapping("/program/{programId}")
    public ResponseEntity<?> programLessonProgress(
            @PathVariable Long programId,
            @AuthenticationPrincipal UserDetails ud) {
        try {
            return ResponseEntity.ok(
                progressService.getLessonProgressForProgram(ud.getUsername(), programId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/progress/program/{programId}/summary
     * Overall % complete, total/completed lesson count, last accessed lesson.
     */
    @GetMapping("/program/{programId}/summary")
    public ResponseEntity<?> programSummary(
            @PathVariable Long programId,
            @AuthenticationPrincipal UserDetails ud) {
        try {
            return ResponseEntity.ok(
                progressService.getProgramProgress(ud.getUsername(), programId)
                    .orElse(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/progress/all
     * Progress summary across all programs the user has accessed.
     */
    @GetMapping("/all")
    public ResponseEntity<?> allProgress(@AuthenticationPrincipal UserDetails ud) {
        try {
            return ResponseEntity.ok(progressService.getAllProgressForUser(ud.getUsername()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/progress/in-progress
     * Programs started but not finished — "Continue Learning" section.
     */
    @GetMapping("/in-progress")
    public ResponseEntity<?> inProgress(@AuthenticationPrincipal UserDetails ud) {
        try {
            return ResponseEntity.ok(progressService.getInProgressPrograms(ud.getUsername()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/progress/resume
     * The single most recently accessed lesson — for the "Resume" button.
     */
    @GetMapping("/resume")
    public ResponseEntity<?> resume(@AuthenticationPrincipal UserDetails ud) {
        try {
            return ResponseEntity.ok(
                progressService.getLastAccessedLesson(ud.getUsername()).orElse(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
