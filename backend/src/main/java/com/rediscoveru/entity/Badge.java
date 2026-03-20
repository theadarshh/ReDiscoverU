package com.rediscoveru.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Badge — definition of a badge that can be earned.
 * Created by admin or seeded at startup.
 */
@Entity
@Table(name = "badges")
@Data
public class Badge {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** URL to badge icon (emoji or image) */
    @Column(length = 500)
    private String iconUrl;

    /** What triggers this badge */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BadgeType badgeType;

    /**
     * Threshold value for the badge trigger.
     * e.g. for STREAK_DAYS: threshold=7 means "7-day streak"
     *      for LESSONS_COMPLETED: threshold=10 means "10 lessons done"
     *      for PROGRAM_COMPLETE: threshold=1 means "1 program completed"
     */
    @Column(name = "threshold_value", nullable = false)
    private int thresholdValue = 1;

    public enum BadgeType {
        FIRST_LESSON,        // Completed first lesson
        LESSONS_COMPLETED,   // N lessons completed
        PROGRAM_COMPLETE,    // N programs fully completed
        STREAK_DAYS,         // N-day consecutive streak
        NOTE_TAKER,          // Created N notes
        BOOKMARKS_ADDED,     // Added N bookmarks
        COMMUNITY_ACTIVE     // Posted N comments
    }
}
