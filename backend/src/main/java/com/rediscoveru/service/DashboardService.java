package com.rediscoveru.service;

import com.rediscoveru.dto.DashboardResponse;
import com.rediscoveru.entity.*;
import com.rediscoveru.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    // ── DB-backed repositories ────────────────────────────────────
    private final UserRepository                userRepo;
    private final ProgramProgressRepository     programProgressRepo;
    private final LessonProgressRepository      lessonProgressRepo;
    private final LiveSessionRepository         sessionRepo;
    private final AnnouncementRepository        announcementRepo;
    private final InAppNotificationRepository   notifRepo;
    private final UserStreakRepository          streakRepo;
    private final UserBadgeRepository           userBadgeRepo;
    private final BookmarkRepository            bookmarkRepo;
    private final UserNoteRepository            noteRepo;

    // ── File-backed service (no DB) ───────────────────────────────
    private final StaticContentService          staticContent;

    /**
     * Builds the complete dashboard payload for a user.
     *
     * ① DB queries   — user-specific data (progress, streaks, sessions, etc.)
     * ② File reads   — global content (banner, video, weekly focus, images)
     *
     * Both parts are combined and returned in a single response object.
     * Frontend makes ONE call and gets everything.
     */
    public DashboardResponse buildDashboard(String email) {
        User user = userRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
        Long uid = user.getId();

        // ── ① DB-backed sections ──────────────────────────────────

        // Progress summary
        List<ProgramProgress> allProgress =
            programProgressRepo.findByUserIdOrderByUpdatedAtDesc(uid);
        int completedPrograms     = (int) programProgressRepo.countCompleted(uid);
        int totalLessonsCompleted = (int) lessonProgressRepo
            .countByUserIdAndStatus(uid, LessonProgress.ProgressStatus.COMPLETED);

        // Continue learning — up to 3 in-progress programs
        List<ProgramProgress> inProgress = programProgressRepo.findInProgressByUser(uid);
        List<Map<String, Object>> continueLearning = new ArrayList<>();
        for (ProgramProgress pp : inProgress.subList(0, Math.min(3, inProgress.size()))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("programId",       pp.getProgram().getId());
            m.put("programTitle",    pp.getProgram().getTitle());
            m.put("overallPct",      pp.getOverallPct());
            m.put("completedLessons", pp.getCompletedLessons());
            m.put("totalLessons",    pp.getTotalLessons());
            m.put("lastContentId",   pp.getLastContentId());
            m.put("lastContentType", pp.getLastContentType());
            m.put("lastContentTitle",pp.getLastContentTitle());
            m.put("updatedAt",       pp.getUpdatedAt());
            continueLearning.add(m);
        }

        // Resume — most recently accessed lesson
        Map<String, Object> resumeLesson = lessonProgressRepo
            .findRecentByUser(uid, PageRequest.of(0, 1))
            .stream().findFirst()
            .map(lp -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("contentId",    lp.getContentId());
                m.put("contentType",  lp.getContentType());
                m.put("contentTitle", lp.getContentTitle());
                m.put("programId",    lp.getProgram().getId());
                m.put("programTitle", lp.getProgram().getTitle());
                m.put("watchedSeconds", lp.getWatchedSeconds());
                m.put("completionPct",  lp.getCompletionPct());
                m.put("status",         lp.getStatus());
                return m;
            }).orElse(null);

        // Upcoming live sessions
        List<Map<String, Object>> sessions = new ArrayList<>();
        for (LiveSession s : sessionRepo.findAllActiveSessions()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sessionId",     s.getId());
            m.put("title",         s.getTitle());
            m.put("meetingLink",   s.getMeetingLink());
            m.put("recurrenceType",s.getRecurrenceType());
            m.put("startTime",     s.getStartTime() != null ? s.getStartTime().toString() : null);
            m.put("timezone",      s.getTimezone());
            m.put("programTitle",  s.getProgram() != null ? s.getProgram().getTitle() : "");
            sessions.add(m);
        }

        // Announcements — DB-backed (supports multiple, ordering, expiry)
        List<Announcement> activeAnnouncements = announcementRepo
            .findByActiveTrueAndExpiresAtAfterOrderByDisplayOrderAscCreatedAtDesc(LocalDateTime.now());
        List<Map<String, Object>> announcements = new ArrayList<>();
        for (Announcement a : activeAnnouncements.subList(0, Math.min(5, activeAnnouncements.size()))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",        a.getId());
            m.put("title",     a.getTitle());
            m.put("message",   a.getMessage());
            m.put("createdAt", a.getCreatedAt());
            m.put("expiresAt", a.getExpiresAt());
            announcements.add(m);
        }

        // Notifications
        long unreadNotifs = notifRepo.countByUserIdAndReadFalse(uid);

        // Gamification
        int currentStreak = streakRepo.findByUserId(uid)
            .map(UserStreak::getCurrentStreak).orElse(0);
        long totalBadges = userBadgeRepo.countByUserId(uid);
        List<Map<String, Object>> recentBadges = new ArrayList<>();
        for (UserBadge ub : userBadgeRepo.findByUserIdOrderByEarnedAtDesc(uid, PageRequest.of(0, 3))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("badgeName",   ub.getBadge().getName());
            m.put("description", ub.getBadge().getDescription());
            m.put("iconUrl",     ub.getBadge().getIconUrl());
            m.put("earnedAt",    ub.getEarnedAt());
            recentBadges.add(m);
        }

        // Counts
        long totalBookmarks = bookmarkRepo.countByUserId(uid);
        long totalNotes     = noteRepo.countByUserId(uid);

        // ── ② File-backed sections (no DB) ───────────────────────
        // These are the same for every user — loaded from disk files.
        // Admin updates them via PUT/PATCH /api/admin/static/{key}
        // or via POST /api/admin/static/mentor-image etc.

        Map<String, Object> featuredVideo = staticContent.getIfActive(StaticContentService.FEATURED_VIDEO);
        Map<String, Object> banner        = staticContent.getIfActive(StaticContentService.BANNER);
        Map<String, Object> weeklyFocus   = staticContent.getIfActive(StaticContentService.WEEKLY_FOCUS);
        Map<String, Object> highlight     = staticContent.getIfActive(StaticContentService.HIGHLIGHT);
        String mentorImageUrl      = staticContent.getMentorImageUrl();
        String inspirationImageUrl = staticContent.getInspirationImageUrl();

        // ── Combine and return ────────────────────────────────────
        return DashboardResponse.builder()
            // User
            .userName(user.getName())
            .email(user.getEmail())
            .subscriptionStatus(user.getSubscriptionStatus().name())
            // Progress
            .totalProgramsAccessed(allProgress.size())
            .completedPrograms(completedPrograms)
            .totalLessonsCompleted(totalLessonsCompleted)
            .continueLearning(continueLearning)
            .resumeLesson(resumeLesson)
            // Sessions & Announcements
            .upcomingSessions(sessions)
            .recentAnnouncements(announcements)
            // Notifications & Gamification
            .unreadNotifications(unreadNotifs)
            .currentStreak(currentStreak)
            .totalBadges((int) totalBadges)
            .recentBadges(recentBadges)
            // Counts
            .totalBookmarks(totalBookmarks)
            .totalNotes(totalNotes)
            // Static content (file-based)
            .featuredVideo(featuredVideo)
            .banner(banner)
            .weeklyFocus(weeklyFocus)
            .highlight(highlight)
            .mentorImageUrl(mentorImageUrl)
            .inspirationImageUrl(inspirationImageUrl)
            .build();
    }
}
