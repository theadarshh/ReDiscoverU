package com.rediscoveru.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity @Table(name = "mentors") @Data
public class Mentor {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String roleTitle = "Lifestyle & Mindset Coach";

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String email;

    private String imageUrl;

    private String whatsappLink;

    private boolean active = true;

    private boolean deleted = false;

    @Column(name = "display_order")
    private int displayOrder = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
