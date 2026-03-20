package com.rediscoveru.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * InAppNotification — a notification shown in the user's notification bell.
 *
 * Created automatically when:
 *   - Admin posts an announcement
 *   - A new live session is scheduled
 *   - A new program/content is added
 *   - User earns a badge (gamification)
 *   - A comment is replied to
 */
@Entity
@Table(name = "in_app_notifications",
       indexes = {
           @Index(name = "idx_notif_user_read", columnList = "user_id,is_read"),
           @Index(name = "idx_notif_user_date", columnList = "user_id,created_at")
       })
@Data
public class InAppNotification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Notification category — used for icon/color in frontend */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotifType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    /**
     * Deep link: where to navigate when the notification is tapped.
     * e.g. "program/3", "session/7", "announcement/12"
     */
    @Column(length = 300)
    private String link;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public enum NotifType {
        ANNOUNCEMENT,   // Admin announcement
        NEW_SESSION,    // Live session scheduled or updated
        NEW_CONTENT,    // New lesson/file added to a program
        BADGE_EARNED,   // Gamification — badge unlocked
        COMMENT_REPLY,  // Someone replied to user's comment
        SYSTEM          // Generic system message
    }
}
