package com.rediscoveru.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity @Table(name = "whatsapp_groups") @Data
public class WhatsAppGroup {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    private GroupType type = GroupType.GROUP;

    private boolean active = true;

    private String description;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum GroupType { GROUP, CHANNEL }
}
