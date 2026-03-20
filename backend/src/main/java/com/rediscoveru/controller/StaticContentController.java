package com.rediscoveru.controller;

import com.rediscoveru.service.StaticContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * StaticContentController
 * ──────────────────────────────────────────────────────────────────
 * Serves and manages the five file-based replaceable content types.
 * No DB involved. All storage is flat JSON files on disk.
 *
 * PUBLIC endpoints (anyone can read):
 *   GET  /api/static/all                → all active content in one call
 *   GET  /api/static/featured-video     → featured YouTube video
 *   GET  /api/static/banner             → announcement banner (only if active)
 *   GET  /api/static/weekly-focus       → mentor's weekly message
 *   GET  /api/static/highlight          → dashboard highlight card
 *   GET  /api/static/mentor-image       → mentor photo URL
 *   GET  /api/static/daily-inspiration  → today's inspiration image URL
 *
 * ADMIN endpoints (ROLE_ADMIN, protected by SecurityConfig):
 *   PUT    /api/admin/static/{key}                  → full replace JSON content
 *   PATCH  /api/admin/static/{key}                  → partial update JSON content
 *   PATCH  /api/admin/static/{key}/toggle           → toggle active flag
 *   POST   /api/admin/static/mentor-image           → upload/replace mentor photo
 *   POST   /api/admin/static/daily-inspiration      → upload/replace inspiration image
 */
@RestController
@RequiredArgsConstructor
public class StaticContentController {

    private final StaticContentService staticContent;

    // ════════════════ PUBLIC — READ ═══════════════════════════════

    /**
     * GET /api/static/all
     * Single call returns all active static content.
     * Frontend can call this once on page load.
     */
    @GetMapping("/api/static/all")
    public ResponseEntity<?> getAll() {
        try { return ResponseEntity.ok(staticContent.getAllStaticContent()); }
        catch (Exception e) { return ResponseEntity.ok(Map.of()); }
    }

    /**
     * GET /api/static/featured-video
     * Returns { title, youtubeUrl, active, updatedAt }
     * Empty map {} if not active.
     */
    @GetMapping("/api/static/featured-video")
    public ResponseEntity<?> getFeaturedVideo() {
        return ResponseEntity.ok(staticContent.getIfActive(StaticContentService.FEATURED_VIDEO));
    }

    /**
     * GET /api/static/banner
     * Returns { message, ctaText, ctaUrl, active, updatedAt }
     * Empty map {} if banner is switched off.
     */
    @GetMapping("/api/static/banner")
    public ResponseEntity<?> getBanner() {
        return ResponseEntity.ok(staticContent.getIfActive(StaticContentService.BANNER));
    }

    /**
     * GET /api/static/weekly-focus
     * Returns { message, theme, active, updatedAt }
     */
    @GetMapping("/api/static/weekly-focus")
    public ResponseEntity<?> getWeeklyFocus() {
        return ResponseEntity.ok(staticContent.getIfActive(StaticContentService.WEEKLY_FOCUS));
    }

    /**
     * GET /api/static/highlight
     * Returns { title, subtitle, ctaText, ctaUrl, active, updatedAt }
     */
    @GetMapping("/api/static/highlight")
    public ResponseEntity<?> getHighlight() {
        return ResponseEntity.ok(staticContent.getIfActive(StaticContentService.HIGHLIGHT));
    }

    /**
     * GET /api/static/mentor-image
     * Returns { url: "/uploads/static/mentor.jpg" } or { url: null }
     */
    @GetMapping("/api/static/mentor-image")
    public ResponseEntity<?> getMentorImage() {
        String url = staticContent.getMentorImageUrl();
        return ResponseEntity.ok(Map.of("url", url != null ? url : ""));
    }

    /**
     * GET /api/static/daily-inspiration
     * Returns { url: "/uploads/static/daily-inspiration.jpg" } or { url: null }
     */
    @GetMapping("/api/static/daily-inspiration")
    public ResponseEntity<?> getDailyInspiration() {
        String url = staticContent.getInspirationImageUrl();
        return ResponseEntity.ok(Map.of("url", url != null ? url : ""));
    }

    // ════════════════ ADMIN — WRITE ═══════════════════════════════

    /**
     * PUT /api/admin/static/{key}
     * Full replacement of content for a given key.
     * Key must be one of: featured-video, banner, weekly-focus, highlight
     *
     * Example body for banner:
     * { "message": "New event this Thursday!", "ctaText": "Join", "ctaUrl": "/programs.html", "active": true }
     */
    @PutMapping("/api/admin/static/{key}")
    public ResponseEntity<?> setContent(
            @PathVariable String key,
            @RequestBody Map<String, Object> body) {
        try {
            if (!isValidKey(key))
                return ResponseEntity.badRequest().body(Map.of("error",
                    "Invalid key. Use: featured-video, banner, weekly-focus, highlight"));
            return ResponseEntity.ok(staticContent.set(key, body));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /**
     * PATCH /api/admin/static/{key}
     * Partial update — only provided fields are changed, rest kept as-is.
     * Useful to just change message text without touching other fields.
     *
     * Example: { "message": "Updated weekly focus text" }
     */
    @PatchMapping("/api/admin/static/{key}")
    public ResponseEntity<?> patchContent(
            @PathVariable String key,
            @RequestBody Map<String, Object> body) {
        try {
            if (!isValidKey(key))
                return ResponseEntity.badRequest().body(Map.of("error",
                    "Invalid key. Use: featured-video, banner, weekly-focus, highlight"));
            return ResponseEntity.ok(staticContent.patch(key, body));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /**
     * PATCH /api/admin/static/{key}/toggle
     * Switch active on/off without changing the content.
     * Admin can pre-write a banner and flip it live instantly.
     */
    @PatchMapping("/api/admin/static/{key}/toggle")
    public ResponseEntity<?> toggleContent(@PathVariable String key) {
        try {
            if (!isValidKey(key))
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid key"));
            return ResponseEntity.ok(staticContent.toggleActive(key));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /**
     * POST /api/admin/static/mentor-image
     * Upload a new mentor profile photo.
     * Overwrites the previous image. No history kept.
     * URL stays the same: /uploads/static/mentor.jpg
     *
     * Returns: { url: "/uploads/static/mentor.jpg", message: "Mentor image updated" }
     */
    @PostMapping("/api/admin/static/mentor-image")
    public ResponseEntity<?> uploadMentorImage(@RequestParam("image") MultipartFile file) {
        try {
            if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
            String url = staticContent.saveMentorImage(file);
            return ResponseEntity.ok(Map.of("url", url, "message", "Mentor image updated"));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /**
     * POST /api/admin/static/daily-inspiration
     * Upload today's inspiration image.
     * Overwrites previous image. No history.
     * URL stays the same: /uploads/static/daily-inspiration.jpg
     *
     * Returns: { url: "/uploads/static/daily-inspiration.jpg", message: "Daily inspiration updated" }
     */
    @PostMapping("/api/admin/static/daily-inspiration")
    public ResponseEntity<?> uploadDailyInspiration(@RequestParam("image") MultipartFile file) {
        try {
            if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
            String url = staticContent.saveDailyInspirationImage(file);
            return ResponseEntity.ok(Map.of("url", url, "message", "Daily inspiration updated"));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /**
     * GET /api/admin/static/{key}
     * Admin read — returns raw content including inactive items (no filter).
     */
    @GetMapping("/api/admin/static/{key}")
    public ResponseEntity<?> adminGetContent(@PathVariable String key) {
        try {
            if (!isValidKey(key))
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid key"));
            return ResponseEntity.ok(staticContent.get(key));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── Key validation ────────────────────────────────────────────
    private boolean isValidKey(String key) {
        return StaticContentService.FEATURED_VIDEO.equals(key)
            || StaticContentService.BANNER.equals(key)
            || StaticContentService.WEEKLY_FOCUS.equals(key)
            || StaticContentService.HIGHLIGHT.equals(key);
    }
}
