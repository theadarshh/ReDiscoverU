package com.rediscoveru.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * DashboardResponse
 * ─────────────────────────────────────────────────────────────────
 * Single payload returned by GET /api/dashboard/home.
 * One HTTP call gives the frontend everything it needs to render
 * the complete personalised dashboard.
 *
 * Split into two categories:
 *
 *  ① DB-backed data  — user-specific, changes per-user
 *     progress, sessions, announcements, streaks, badges, etc.
 *
 *  ② File-backed data — global, same for every user
 *     featuredVideo, banner, weeklyFocus, highlight, image URLs
 *     No DB query needed. Read from flat JSON files on disk.
 */
@Data
@Builder
public class DashboardResponse {

    // ─────────────────────────────────────────────────────────────
    // ① USER DATA  (DB-backed — per-user)
    // ─────────────────────────────────────────────────────────────

    // User info ───────────────────────────────────────────────────
    private String userName;
    private String email;
    private String subscriptionStatus;

    // Progress summary ────────────────────────────────────────────
    private int totalProgramsAccessed;
    private int completedPrograms;
    private int totalLessonsCompleted;

    // Continue learning — up to 3 in-progress programs ────────────
    private List<Map<String, Object>> continueLearning;

    // Resume button — most recently accessed lesson ───────────────
    private Map<String, Object> resumeLesson;

    // Upcoming live sessions ──────────────────────────────────────
    private List<Map<String, Object>> upcomingSessions;

    // Recent announcements (DB — has expiry, ordering, history) ───
    private List<Map<String, Object>> recentAnnouncements;

    // Notification bell count ─────────────────────────────────────
    private long unreadNotifications;

    // Gamification ────────────────────────────────────────────────
    private int currentStreak;
    private int totalBadges;
    private List<Map<String, Object>> recentBadges;

    // Bookmarks & Notes counts ────────────────────────────────────
    private long totalBookmarks;
    private long totalNotes;

    // ─────────────────────────────────────────────────────────────
    // ② STATIC CONTENT  (File-backed — global, same for all users)
    //    Loaded from uploads/static/*.json  — no DB, no entity.
    //    Admin overwrites these files via /api/admin/static/*.
    // ─────────────────────────────────────────────────────────────

    /**
     * Featured video of the day.
     * Shape: { title, youtubeUrl, active, updatedAt }
     * Empty map {} when active=false — frontend skips rendering.
     */
    private Map<String, Object> featuredVideo;

    /**
     * Top-of-page announcement banner.
     * Shape: { message, ctaText, ctaUrl, active, updatedAt }
     * Empty map {} when active=false — frontend hides the bar.
     */
    private Map<String, Object> banner;

    /**
     * Mentor's weekly focus message card.
     * Shape: { message, theme, active, updatedAt }
     */
    private Map<String, Object> weeklyFocus;

    /**
     * Hero highlight card shown on the user dashboard.
     * Shape: { title, subtitle, ctaText, ctaUrl, active, updatedAt }
     */
    private Map<String, Object> highlight;

    /**
     * Mentor profile image URL — served as a static file.
     * Value: "/uploads/static/mentor.jpg"  OR  null if not uploaded yet.
     * Frontend: <img src="${API}${mentorImageUrl}">
     */
    private String mentorImageUrl;

    /**
     * Daily inspiration image URL — overwritten each day by admin.
     * Value: "/uploads/static/daily-inspiration.jpg"  OR  null.
     */
    private String inspirationImageUrl;
}
