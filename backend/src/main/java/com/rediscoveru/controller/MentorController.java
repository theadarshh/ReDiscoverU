package com.rediscoveru.controller;

import com.rediscoveru.entity.Mentor;
import com.rediscoveru.service.MentorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MentorController — public-only endpoints.
 * All admin mentor management is handled by AdminController.
 */
@RestController
@RequiredArgsConstructor
public class MentorController {

    private final MentorService mentorService;

    // ── Public endpoint only ───────────────────────────────────────
    @GetMapping("/api/homepage/mentors")
    public ResponseEntity<List<Mentor>> publicMentors() {
        try { return ResponseEntity.ok(mentorService.getActive()); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }
}
