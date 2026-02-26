package com.rediscoveru.controller;

import com.rediscoveru.entity.Mentor;
import com.rediscoveru.service.MentorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController @RequiredArgsConstructor
public class MentorController {

    private final MentorService mentorService;

    // ── Public ─────────────────────────────────────────────────────
    @GetMapping("/api/homepage/mentors")
    public ResponseEntity<List<Mentor>> publicMentors() {
        try { return ResponseEntity.ok(mentorService.getActive()); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    // ── Admin ──────────────────────────────────────────────────────
    @GetMapping("/api/admin/mentors")
    public ResponseEntity<List<Mentor>> all() {
        try { return ResponseEntity.ok(mentorService.getAll()); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @PostMapping("/api/admin/mentors")
    public ResponseEntity<?> create(@RequestBody Mentor m) {
        try { return ResponseEntity.ok(mentorService.create(m)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/api/admin/mentors/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Mentor m) {
        try { return ResponseEntity.ok(mentorService.update(id, m)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PatchMapping("/api/admin/mentors/{id}/toggle")
    public ResponseEntity<?> toggle(@PathVariable Long id) {
        try { return ResponseEntity.ok(mentorService.toggle(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/api/admin/mentors/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try { mentorService.delete(id); return ResponseEntity.ok(Map.of("deleted", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/api/admin/mentors/{id}/image")
    public ResponseEntity<?> uploadImage(@PathVariable Long id, @RequestParam("image") MultipartFile file) {
        try { return ResponseEntity.ok(mentorService.uploadImage(id, file)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
}
