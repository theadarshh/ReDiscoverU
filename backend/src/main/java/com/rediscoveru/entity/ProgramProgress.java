package com.rediscoveru.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ProgramProgress — one row per (user, program).
 * Aggregates LessonProgress rows into an overall percentage and
 * stores the last-accessed lesson for "Continue Learning".
 */
@Entity
@Table(name = "program_progress",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_program_progress",
           columnNames = {"user_id", "program_id"}))
@Data
public class ProgramProgress {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    /** 0–100 computed from completed lessons / total lessons */
    private Integer overallPct = 0;

    private Integer totalLessons     = 0;
    private Integer completedLessons = 0;

    /** Last-accessed content for "Resume" button */
    private Long   lastContentId;
    private String lastContentType;
    private String lastContentTitle;

    /** When the program was started (first lesson accessed) */
    private LocalDateTime startedAt;

    /** When overallPct first reached 100 */
    private LocalDateTime completedAt;

    private LocalDateTime updatedAt = LocalDateTime.now();
}
