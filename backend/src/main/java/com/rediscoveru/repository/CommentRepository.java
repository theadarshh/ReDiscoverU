package com.rediscoveru.repository;

import com.rediscoveru.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** Top-level comments on a content item, newest first */
    @Query("SELECT c FROM Comment c WHERE c.program.id = :pid " +
           "AND c.contentId = :cid AND c.contentType = :ctype " +
           "AND c.parentId IS NULL AND c.deleted = false " +
           "ORDER BY c.createdAt DESC")
    List<Comment> findTopLevelComments(
        @Param("pid") Long programId,
        @Param("cid") Long contentId,
        @Param("ctype") String contentType);

    /** Replies to a specific comment */
    @Query("SELECT c FROM Comment c WHERE c.parentId = :parentId " +
           "AND c.deleted = false ORDER BY c.createdAt ASC")
    List<Comment> findReplies(@Param("parentId") Long parentId);

    /** All top-level comments for a program (admin view) */
    @Query("SELECT c FROM Comment c WHERE c.program.id = :pid " +
           "AND c.parentId IS NULL AND c.deleted = false " +
           "ORDER BY c.createdAt DESC")
    List<Comment> findByProgramId(@Param("pid") Long programId);

    long countByProgramIdAndDeletedFalse(Long programId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.program.id = :pid " +
           "AND c.contentId = :cid AND c.contentType = :ctype " +
           "AND c.parentId IS NULL AND c.deleted = false")
    long countTopLevelComments(
        @Param("pid") Long programId,
        @Param("cid") Long contentId,
        @Param("ctype") String contentType);
}
