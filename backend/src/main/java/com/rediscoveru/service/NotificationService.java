package com.rediscoveru.service;

import com.rediscoveru.entity.*;
import com.rediscoveru.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final InAppNotificationRepository notifRepo;
    private final UserRepository              userRepo;

    // ── Create single notification ────────────────────────────────────
    @Transactional
    public InAppNotification createForUser(User user,
                                           InAppNotification.NotifType type,
                                           String title,
                                           String message,
                                           String link) {
        InAppNotification n = new InAppNotification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setLink(link);
        n.setCreatedAt(LocalDateTime.now());
        return notifRepo.save(n);
    }

    /**
     * Broadcast a notification to ALL paid users.
     * Called async — does not block the calling thread.
     */
    @Async
    @Transactional
    public void broadcastToPaidUsers(InAppNotification.NotifType type,
                                     String title,
                                     String message,
                                     String link) {
        List<User> paidUsers = userRepo.findBySubscriptionStatus(User.SubscriptionStatus.PAID);
        for (User user : paidUsers) {
            try { createForUser(user, type, title, message, link); }
            catch (Exception e) { /* non-fatal */ }
        }
    }

    // ── Convenience broadcast methods used by other services ─────────

    /** Called when admin posts an announcement */
    public void onAnnouncement(String announcementTitle) {
        broadcastToPaidUsers(
            InAppNotification.NotifType.ANNOUNCEMENT,
            "New Announcement",
            announcementTitle,
            "announcements"
        );
    }

    /** Called when a live session is created or updated */
    public void onNewSession(LiveSession session) {
        broadcastToPaidUsers(
            InAppNotification.NotifType.NEW_SESSION,
            "Live Session Scheduled",
            session.getTitle(),
            "sessions"
        );
    }

    /** Called when new content is added to a program */
    public void onNewContent(String contentTitle, Long programId) {
        broadcastToPaidUsers(
            InAppNotification.NotifType.NEW_CONTENT,
            "New Content Added",
            contentTitle,
            "program/" + programId
        );
    }

    /** Called when a badge is earned — single user */
    public void onBadgeEarned(User user, String badgeName) {
        createForUser(user,
            InAppNotification.NotifType.BADGE_EARNED,
            "Badge Earned! 🏅",
            "You earned the \"" + badgeName + "\" badge!",
            "achievements"
        );
    }

    /** Called when someone replies to a comment */
    public void onCommentReply(User recipient, String programTitle) {
        createForUser(recipient,
            InAppNotification.NotifType.COMMENT_REPLY,
            "Someone replied to your comment",
            "New reply in " + programTitle,
            "program/" + null  // filled in by caller
        );
    }

    // ── User-facing queries ───────────────────────────────────────────

    public List<InAppNotification> getRecentNotifications(String email, int limit) {
        User user = resolveUser(email);
        return notifRepo.findByUserIdOrderByCreatedAtDesc(user.getId(),
            PageRequest.of(0, Math.min(limit, 50)));
    }

    public List<InAppNotification> getUnreadNotifications(String email) {
        return notifRepo.findByUserIdAndReadFalseOrderByCreatedAtDesc(resolveUser(email).getId());
    }

    public long getUnreadCount(String email) {
        return notifRepo.countByUserIdAndReadFalse(resolveUser(email).getId());
    }

    @Transactional
    public void markAsRead(String email, Long notifId) {
        User user = resolveUser(email);
        notifRepo.findByIdAndUserId(notifId, user.getId()).ifPresent(n -> {
            n.setRead(true);
            n.setReadAt(LocalDateTime.now());
            notifRepo.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(String email) {
        notifRepo.markAllRead(resolveUser(email).getId(), LocalDateTime.now());
    }

    @Transactional
    public void deleteNotification(String email, Long notifId) {
        User user = resolveUser(email);
        notifRepo.findByIdAndUserId(notifId, user.getId())
            .ifPresent(notifRepo::delete);
    }

    public Map<String, Object> getNotificationSummary(String email) {
        long unread = getUnreadCount(email);
        List<InAppNotification> recent = getRecentNotifications(email, 5);
        return Map.of("unreadCount", unread, "recent", recent);
    }

    // ── Scheduled cleanup — runs daily at 3 AM ────────────────────────
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanOldNotifications() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
            notifRepo.deleteOldRead(cutoff);
        } catch (Exception e) {
            System.err.println("[NotifCleanup] Failed: " + e.getMessage());
        }
    }

    private User resolveUser(String email) {
        return userRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}
