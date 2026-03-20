package com.rediscoveru.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * UserStreak — tracks consecutive daily learning activity.
 * A streak is incremented when a user completes or accesses any lesson
 * on a day they have not already been active.
 * Missing a day resets currentStreak to 0 (longestStreak preserved).
 */
@Entity
@Table(name = "user_streaks")
@Data
public class UserStreak {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak = 0;

    @Column(name = "longest_streak", nullable = false)
    private int longestStreak = 0;

    @Column(name = "total_active_days", nullable = false)
    private int totalActiveDays = 0;

    /** The last date the user was active (used to determine streak continuity) */
    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    private LocalDateTime updatedAt = LocalDateTime.now();
}
