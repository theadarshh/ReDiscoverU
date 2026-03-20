package com.rediscoveru.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Bookmark — marks a content item (or a specific timestamp within a video)
 * for quick reference. One user can bookmark the same content multiple times
 * at different timestamps, but a content-level bookmark (timestampSeconds=null)
 * is unique per (user, contentId, contentType).
 */
@Entity
@Table(name = "bookmarks",
       indexes = {
           @Index(name = "idx_bm_user_program", columnList = "user_id,program_id"),
           @Index(name = "idx_bm_content",      columnList = "user_id,content_id,content_type")
       })
@Data
public class Bookmark {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    @Column(name = "content_title", length = 300)
    private String contentTitle;

    /** Optional: video timestamp this bookmark points to (in seconds) */
    @Column(name = "timestamp_seconds")
    private Integer timestampSeconds;

    /** Optional label the user gives to this bookmark */
    @Column(length = 200)
    private String label;

    private LocalDateTime createdAt = LocalDateTime.now();
}
