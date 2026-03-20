package com.rediscoveru.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * UserNote — a private note written by a user on a specific content item.
 * A "content item" is either a ProgramContent (contentType="CONTENT")
 * or an UploadedFile (contentType="FILE").
 */
@Entity
@Table(name = "user_notes",
       indexes = {
           @Index(name = "idx_notes_user_program", columnList = "user_id,program_id"),
           @Index(name = "idx_notes_content",      columnList = "user_id,content_id,content_type")
       })
@Data
public class UserNote {

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

    /** "CONTENT" | "FILE" */
    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    /** Snapshot of content title for display even if content is deleted */
    @Column(name = "content_title", length = 300)
    private String contentTitle;

    /** Optional: video timestamp in seconds the note refers to */
    @Column(name = "timestamp_seconds")
    private Integer timestampSeconds;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String noteText;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
