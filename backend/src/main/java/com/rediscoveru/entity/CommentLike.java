package com.rediscoveru.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/** CommentLike — a like on a comment. Unique per (user, comment). */
@Entity
@Table(name = "comment_likes",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_comment_like",
           columnNames = {"user_id", "comment_id"}))
@Data
public class CommentLike {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    private LocalDateTime createdAt = LocalDateTime.now();
}
