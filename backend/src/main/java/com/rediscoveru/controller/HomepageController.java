package com.rediscoveru.controller;

import com.rediscoveru.entity.HomepageVideo;
import com.rediscoveru.entity.MentorQuote;
import com.rediscoveru.entity.WhatsAppGroup;
import com.rediscoveru.entity.MotivationPost;
import com.rediscoveru.repository.HomepageVideoRepository;
import com.rediscoveru.repository.MentorQuoteRepository;
import com.rediscoveru.repository.WhatsAppGroupRepository;
import com.rediscoveru.repository.MotivationPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HomepageController {

    private final HomepageVideoRepository  videoRepo;
    private final MentorQuoteRepository    quoteRepo;
    private final WhatsAppGroupRepository  waGroupRepo;
    private final MotivationPostRepository motivationRepo;

    // ── Public endpoints ──────────────────────────────────────────
    @GetMapping("/api/homepage/videos")
    public ResponseEntity<?> publicVideos() {
        try { return ResponseEntity.ok(videoRepo.findByActiveTrueOrderByDisplayOrderAscCreatedAtAsc()); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @GetMapping("/api/homepage/quote")
    public ResponseEntity<?> activeQuote() {
        try {
            return quoteRepo.findFirstByActiveTrueOrderByCreatedAtDesc()
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.noContent().build());
        } catch (Exception e) { return ResponseEntity.noContent().build(); }
    }

    @GetMapping("/api/homepage/whatsapp-groups")
    public ResponseEntity<?> publicWAGroups() {
        try { return ResponseEntity.ok(waGroupRepo.findByActiveTrueOrderByCreatedAtAsc()); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @GetMapping("/api/homepage/motivation-posts")
    public ResponseEntity<?> publicMotivationPosts() {
        try { return ResponseEntity.ok(motivationRepo.findByActiveTrueOrderByCreatedAtDesc()); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    // ── Admin — homepage videos ────────────────────────────────────
    @GetMapping("/api/admin/homepage-video")
    public ResponseEntity<?> adminVideoList() {
        try { return ResponseEntity.ok(videoRepo.findAllByOrderByDisplayOrderAscCreatedAtAsc()); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @PostMapping("/api/admin/homepage-video")
    public ResponseEntity<?> createVideo(@RequestBody HomepageVideo v) {
        try {
            if (v.getTitle() == null || v.getTitle().isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Title is required"));
            if (v.getYoutubeUrl() == null || v.getYoutubeUrl().isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "YouTube URL is required"));
            return ResponseEntity.ok(videoRepo.save(v));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/api/admin/homepage-video/{id}")
    public ResponseEntity<?> updateVideo(@PathVariable Long id, @RequestBody HomepageVideo updated) {
        try {
            HomepageVideo v = videoRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Video not found"));
            if (updated.getTitle() != null)      v.setTitle(updated.getTitle());
            if (updated.getYoutubeUrl() != null) v.setYoutubeUrl(updated.getYoutubeUrl());
            v.setActive(updated.isActive());
            v.setDisplayOrder(updated.getDisplayOrder());
            return ResponseEntity.ok(videoRepo.save(v));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/api/admin/homepage-video/{id}")
    public ResponseEntity<?> deleteVideo(@PathVariable Long id) {
        try { videoRepo.deleteById(id); return ResponseEntity.ok(Map.of("deleted", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PatchMapping("/api/admin/homepage-video/{id}/toggle")
    public ResponseEntity<?> toggleVideo(@PathVariable Long id) {
        try {
            HomepageVideo v = videoRepo.findById(id).orElseThrow();
            v.setActive(!v.isActive());
            videoRepo.save(v);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── Admin — mentor quotes ──────────────────────────────────────
    @GetMapping("/api/admin/quotes")
    public ResponseEntity<?> adminQuoteList() {
        try { return ResponseEntity.ok(quoteRepo.findAllByOrderByCreatedAtDesc()); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @PostMapping("/api/admin/quotes")
    public ResponseEntity<?> createQuote(@RequestBody MentorQuote q) {
        try {
            if (q.getQuoteText() == null || q.getQuoteText().isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Quote text is required"));
            if (q.getAuthorName() == null || q.getAuthorName().isBlank())
                q.setAuthorName("Jayashankar Lingaiah");
            if (q.isActive()) quoteRepo.deactivateAll();
            return ResponseEntity.ok(quoteRepo.save(q));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/api/admin/quotes/{id}")
    public ResponseEntity<?> updateQuote(@PathVariable Long id, @RequestBody MentorQuote updated) {
        try {
            MentorQuote q = quoteRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Quote not found"));
            if (updated.getQuoteText() != null)  q.setQuoteText(updated.getQuoteText());
            if (updated.getAuthorName() != null && !updated.getAuthorName().isBlank())
                q.setAuthorName(updated.getAuthorName());
            if (updated.isActive() && !q.isActive()) quoteRepo.deactivateAll();
            q.setActive(updated.isActive());
            return ResponseEntity.ok(quoteRepo.save(q));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/api/admin/quotes/{id}")
    public ResponseEntity<?> deleteQuote(@PathVariable Long id) {
        try { quoteRepo.deleteById(id); return ResponseEntity.ok(Map.of("deleted", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PatchMapping("/api/admin/quotes/{id}/activate")
    public ResponseEntity<?> activateQuote(@PathVariable Long id) {
        try {
            quoteRepo.deactivateAll();
            MentorQuote q = quoteRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Quote not found"));
            q.setActive(true);
            return ResponseEntity.ok(quoteRepo.save(q));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
}
