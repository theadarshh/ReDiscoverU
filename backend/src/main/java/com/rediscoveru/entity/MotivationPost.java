package com.rediscoveru.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity @Table(name = "motivation_posts") @Data
public class MotivationPost {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Relative path to saved image, or external URL */
    @Column(columnDefinition = "TEXT")
    private String imagePath;

    @Column(columnDefinition = "TEXT")
    private String caption;

    private boolean active = true;

    private LocalDateTime createdAt = LocalDateTime.now();
}
