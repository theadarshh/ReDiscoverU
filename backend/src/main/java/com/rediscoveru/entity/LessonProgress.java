package com.rediscoveru.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * LessonProgress — tracks a single user's progress on a single content item.
 *
 * A "lesson" is either a ProgramContent (URL-based) or UploadedFile (video/PDF).
 * contentType distinguishes them:  "CONTENT" or "FILE"
 *
 * Unique constraint: one record per (user, contentId, contentType).
 */
@Entity
@Table(name = "lesson_progress",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_lesson_progress",
           columnNames = {"user_id", "content_id", "content_type"}))
@Data
public class LessonProgress {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    /** ID of ProgramContent or UploadedFile */
    @Column(name = "content_id", nullable = false)
    private Long contentId;

    /** "CONTENT" = ProgramContent, "FILE" = UploadedFile */
    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    /** Title snapshot (so it shows even if content is deleted) */
    @Column(name = "content_title", length = 300)
    private String contentTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgressStatus status = ProgressStatus.NOT_STARTED;

    /** For video content: seconds watched / total seconds */
    private Integer watchedSeconds = 0;
    private Integer totalSeconds   = 0;

    /** Completion percentage 0–100 */
    private Integer completionPct = 0;

    /** When user last accessed this lesson */
    private LocalDateTime lastAccessedAt;

    /** When user marked/auto-completed */
    private LocalDateTime completedAt;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ProgressStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }
}
