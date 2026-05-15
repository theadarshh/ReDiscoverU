package com.rediscoveru.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity @Table(name = "program_content") @Data
public class ProgramContent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    private Program program;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    private ContentType contentType;

    @Column(columnDefinition = "TEXT")
    private String contentUrl;

    /** For MEETING type: e.g. "Tue & Thu – 8:00 PM IST" */
    private String scheduleText;

    private Integer orderIndex = 0;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ContentType { VIDEO, PDF, ARTICLE, YOUTUBE, MEETING, LINK }
}
