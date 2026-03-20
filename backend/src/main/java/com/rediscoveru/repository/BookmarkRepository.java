package com.rediscoveru.repository;

import com.rediscoveru.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    List<Bookmark> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Bookmark> findByUserIdAndProgramIdOrderByCreatedAtDesc(Long userId, Long programId);

    List<Bookmark> findByUserIdAndContentIdAndContentTypeOrderByTimestampSecondsAsc(
        Long userId, Long contentId, String contentType);

    /** Content-level bookmark (no timestamp) — unique per user+content */
    @Query("SELECT b FROM Bookmark b WHERE b.user.id = :uid " +
           "AND b.contentId = :cid AND b.contentType = :ctype AND b.timestampSeconds IS NULL")
    Optional<Bookmark> findContentBookmark(
        @Param("uid") Long userId,
        @Param("cid") Long contentId,
        @Param("ctype") String contentType);

    boolean existsByUserIdAndContentIdAndContentTypeAndTimestampSecondsIsNull(
        Long userId, Long contentId, String contentType);

    Optional<Bookmark> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
