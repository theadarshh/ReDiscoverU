package com.rediscoveru.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity @Table(name = "mentor_quotes") @Data
public class MentorQuote {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String quoteText;

    @Column(nullable = false)
    private String authorName = "Jayashankar Lingaiah";

    private boolean active = false;

    private LocalDateTime createdAt = LocalDateTime.now();
}
