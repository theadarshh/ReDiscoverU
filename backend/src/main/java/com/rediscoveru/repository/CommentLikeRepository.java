package com.rediscoveru.repository;

import com.rediscoveru.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByUserIdAndCommentId(Long userId, Long commentId);

    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    /** IDs of comments liked by a user — used to annotate responses */
    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.user.id = :uid " +
           "AND cl.comment.id IN :commentIds")
    List<Long> findLikedCommentIds(@Param("uid") Long userId,
                                   @Param("commentIds") List<Long> commentIds);

    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id = :commentId")
    void deleteByCommentId(@Param("commentId") Long commentId);
}
