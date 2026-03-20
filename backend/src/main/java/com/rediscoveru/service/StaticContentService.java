package com.rediscoveru.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * StaticContentService
 * ──────────────────────────────────────────────────────────────────
 * Manages five types of replaceable single-item content using plain
 * JSON files on disk. No entity, no repository, no DB table.
 *
 * Philosophy: UPLOAD → OVERWRITE → DISPLAY
 *   - Admin writes a new value → it immediately replaces the old one
 *   - No history, no versioning, no querying
 *   - Content is served from disk on every GET (fast, no DB round-trip)
 *
 * Files stored at:  {uploadDir}/static/
 *
 *   featured-video.json   → YouTube URL + title shown on homepage
 *   banner.json           → single announcement banner (on/off)
 *   weekly-focus.json     → mentor's message for the week
 *   highlight.json        → hero highlight card on user dashboard
 *   daily-inspiration.jpg → single image, overwritten each day
 *   mentor.jpg            → mentor profile photo, overwritten anytime
 *
 * JSON format is a simple Map<String,Object>. No schema required.
 * Missing keys return sensible defaults. Missing files return defaults.
 */
@Service
public class StaticContentService {

    @Value("${file.upload.dir:uploads}") private String uploadDir;

    private final ObjectMapper mapper;
    private Path staticDir;

    // ── Key constants — used as file names ────────────────────────
    public static final String FEATURED_VIDEO = "featured-video";
    public static final String BANNER         = "banner";
    public static final String WEEKLY_FOCUS   = "weekly-focus";
    public static final String HIGHLIGHT      = "highlight";

    public StaticContentService() {
        this.mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostConstruct
    public void init() {
        staticDir = Paths.get(uploadDir, "static");
        try {
            Files.createDirectories(staticDir);
            // Seed default files if they don't exist
            seedDefault(FEATURED_VIDEO, Map.of(
                "title",      "Rediscover Your Purpose",
                "youtubeUrl", "https://youtu.be/975zQXVgu0w",
                "active",     true,
                "updatedAt",  LocalDateTime.now().toString()
            ));
            seedDefault(BANNER, Map.of(
                "message",  "Welcome to ReDiscoverU — your growth starts here.",
                "ctaText",  "Explore Programs",
                "ctaUrl",   "/programs.html",
                "active",   false,
                "updatedAt", LocalDateTime.now().toString()
            ));
            seedDefault(WEEKLY_FOCUS, Map.of(
                "message",   "This week, focus on building one small consistent habit.",
                "theme",     "Discipline",
                "active",    true,
                "updatedAt", LocalDateTime.now().toString()
            ));
            seedDefault(HIGHLIGHT, Map.of(
                "title",     "Begin Your Journey",
                "subtitle",  "Structured growth, one session at a time.",
                "ctaText",   "View Programs",
                "ctaUrl",    "/programs.html",
                "active",    true,
                "updatedAt", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            System.err.println("[StaticContent] Init failed: " + e.getMessage());
        }
    }

    // ── Read ──────────────────────────────────────────────────────

    /**
     * Returns the content for the given key.
     * Returns a default empty map (never null) if the file doesn't exist.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String key) {
        Path file = staticDir.resolve(key + ".json");
        if (!Files.exists(file)) return new LinkedHashMap<>();
        try {
            return mapper.readValue(file.toFile(), Map.class);
        } catch (Exception e) {
            System.err.println("[StaticContent] Read error for " + key + ": " + e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    /**
     * Get only if active=true, else return empty map.
     * Lets frontend skip rendering easily.
     */
    public Map<String, Object> getIfActive(String key) {
        Map<String, Object> content = get(key);
        if (Boolean.TRUE.equals(content.get("active"))) return content;
        return new LinkedHashMap<>();
    }

    /**
     * Convenience: get all four JSON-based content items in one call.
     * Used by DashboardService to bundle into the dashboard response.
     */
    public Map<String, Object> getAllStaticContent() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("featuredVideo",  getIfActive(FEATURED_VIDEO));
        result.put("banner",         getIfActive(BANNER));
        result.put("weeklyFocus",    getIfActive(WEEKLY_FOCUS));
        result.put("highlight",      getIfActive(HIGHLIGHT));
        result.put("mentorImageUrl",     getMentorImageUrl());
        result.put("inspirationImageUrl", getInspirationImageUrl());
        return result;
    }

    // ── Write ─────────────────────────────────────────────────────

    /**
     * Completely replaces the content for the given key.
     * The incoming map is merged with updatedAt and written atomically.
     */
    public Map<String, Object> set(String key, Map<String, Object> incoming) {
        incoming.put("updatedAt", LocalDateTime.now().toString());
        write(key, incoming);
        return incoming;
    }

    /**
     * Partial update: only the fields present in the patch are updated.
     * Fields not in the patch retain their current values.
     */
    public Map<String, Object> patch(String key, Map<String, Object> patch) {
        Map<String, Object> current = get(key);
        current.putAll(patch);
        current.put("updatedAt", LocalDateTime.now().toString());
        write(key, current);
        return current;
    }

    /**
     * Toggle the active flag for a content key.
     */
    public Map<String, Object> toggleActive(String key) {
        Map<String, Object> current = get(key);
        boolean nowActive = !Boolean.TRUE.equals(current.get("active"));
        current.put("active", nowActive);
        current.put("updatedAt", LocalDateTime.now().toString());
        write(key, current);
        return current;
    }

    // ── Image files ───────────────────────────────────────────────

    /**
     * Overwrites the single mentor profile image.
     * Always saved as mentor.jpg (fixed name = fixed URL).
     * Old file is replaced; no previous copy is kept.
     */
    public String saveMentorImage(MultipartFile file) throws Exception {
        return saveImage(file, "mentor");
    }

    /**
     * Overwrites the daily inspiration image.
     * Always saved as daily-inspiration.jpg.
     */
    public String saveDailyInspirationImage(MultipartFile file) throws Exception {
        return saveImage(file, "daily-inspiration");
    }

    public String getMentorImageUrl() {
        Path img = staticDir.resolve("mentor.jpg");
        return Files.exists(img) ? "/uploads/static/mentor.jpg" : null;
    }

    public String getInspirationImageUrl() {
        Path img = staticDir.resolve("daily-inspiration.jpg");
        return Files.exists(img) ? "/uploads/static/daily-inspiration.jpg" : null;
    }

    // ── Private helpers ───────────────────────────────────────────

    private String saveImage(MultipartFile file, String baseName) throws Exception {
        String ext = getExtension(file.getOriginalFilename());
        // Always use the fixed name — this enforces replace-only semantics
        String filename = baseName + ext;
        Path dest = staticDir.resolve(filename);
        // Overwrite existing file
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        return "/uploads/static/" + filename;
    }

    private String getExtension(String originalFilename) {
        if (originalFilename == null) return ".jpg";
        int dot = originalFilename.lastIndexOf('.');
        return dot >= 0 ? originalFilename.substring(dot).toLowerCase() : ".jpg";
    }

    private void write(String key, Map<String, Object> data) {
        try {
            Path file = staticDir.resolve(key + ".json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write static content [" + key + "]: " + e.getMessage());
        }
    }

    private void seedDefault(String key, Map<String, Object> defaults) {
        Path file = staticDir.resolve(key + ".json");
        if (!Files.exists(file)) {
            write(key, new LinkedHashMap<>(defaults));
        }
    }
}
