package com.rediscoveru.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/** UserBadge — a badge that has been earned by a specific user. */
@Entity
@Table(name = "user_badges",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_user_badge",
           columnNames = {"user_id", "badge_id"}))
@Data
public class UserBadge {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(name = "earned_at", nullable = false)
    private LocalDateTime earnedAt = LocalDateTime.now();
}
