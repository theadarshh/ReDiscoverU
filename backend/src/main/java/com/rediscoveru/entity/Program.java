package com.rediscoveru.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity @Table(name = "programs") @Data
public class Program {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Optional dynamic category — replaces the old ProgramType enum */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    private ProgramCategory category;

    /** Legacy enum kept for backward compat — new code uses category */
    @Enumerated(EnumType.STRING)
    private ProgramType type;

    private String duration;

    /** Short tagline shown on cards */
    private String tagline;

    /** Cover image URL */
    private String coverImageUrl;

    private boolean active = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ProgramType { SELF_PACED, LIVE, MENTORSHIP }
}
