package com.rediscoveru.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
@Data
public class Announcement {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    private boolean active = true;

    /** Display order — lower number = shown first (admin can reorder) */
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Auto-expiry — 24 hours after creation. Scheduled task deletes after this. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
}
