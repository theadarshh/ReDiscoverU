package com.rediscoveru.repository;

import com.rediscoveru.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByUserIdAndContentIdAndContentType(
        Long userId, Long contentId, String contentType);

    List<LessonProgress> findByUserIdAndProgramId(Long userId, Long programId);

    @Query("SELECT lp FROM LessonProgress lp WHERE lp.user.id = :uid AND lp.program.id = :pid " +
           "AND lp.status = 'COMPLETED' ORDER BY lp.completedAt DESC")
    List<LessonProgress> findCompletedByUserAndProgram(
        @Param("uid") Long userId, @Param("pid") Long programId);

    @Query("SELECT COUNT(lp) FROM LessonProgress lp WHERE lp.user.id = :uid " +
           "AND lp.program.id = :pid AND lp.status = 'COMPLETED'")
    long countCompleted(@Param("uid") Long userId, @Param("pid") Long programId);

    /** Most recently accessed lesson across all programs — for "Continue Learning" */
    @Query("SELECT lp FROM LessonProgress lp WHERE lp.user.id = :uid " +
           "ORDER BY lp.lastAccessedAt DESC")
    List<LessonProgress> findRecentByUser(@Param("uid") Long userId,
        org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(lp) FROM LessonProgress lp WHERE lp.user.id = :uid AND lp.status = :status")
    long countByUserIdAndStatus(@Param("uid") Long userId,
                                @Param("status") LessonProgress.ProgressStatus status);

    /** Global count by status — for admin analytics */
    @Query("SELECT COUNT(lp) FROM LessonProgress lp WHERE lp.status = :status")
    long countByStatus(@Param("status") LessonProgress.ProgressStatus status);

    /** Count distinct users who had any lesson activity since a given date */
    @Query("SELECT COUNT(DISTINCT lp.user.id) FROM LessonProgress lp WHERE lp.lastAccessedAt >= :since")
    long countDistinctUsersSince(@Param("since") java.time.LocalDateTime since);

}
