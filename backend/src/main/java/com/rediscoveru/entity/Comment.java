package com.rediscoveru.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Comment — a comment or reply on a content item (lesson/file).
 * Supports threaded replies via parentId (max 1 level deep).
 *
 * A content item is identified by (programId, contentId, contentType).
 */
@Entity
@Table(name = "comments",
       indexes = {
           @Index(name = "idx_comment_content", columnList = "program_id,content_id,content_type"),
           @Index(name = "idx_comment_parent",  columnList = "parent_id")
       })
@Data
public class Comment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "email"})
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Program program;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;  // "CONTENT" | "FILE"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String commentText;

    /** null = top-level comment; non-null = reply to another comment */
    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    @Column(name = "reply_count", nullable = false)
    private int replyCount = 0;

    private boolean deleted = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Transient — populated by service for API responses */
    @Transient
    private boolean likedByCurrentUser = false;
}
