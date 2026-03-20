package com.rediscoveru.repository;

import com.rediscoveru.entity.ProgramProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProgramProgressRepository extends JpaRepository<ProgramProgress, Long> {

    Optional<ProgramProgress> findByUserIdAndProgramId(Long userId, Long programId);

    List<ProgramProgress> findByUserIdOrderByUpdatedAtDesc(Long userId);

    /** In-progress programs (started but not complete) for "Continue Learning" */
    @Query("SELECT pp FROM ProgramProgress pp WHERE pp.user.id = :uid " +
           "AND pp.overallPct > 0 AND pp.overallPct < 100 " +
           "ORDER BY pp.updatedAt DESC")
    List<ProgramProgress> findInProgressByUser(@Param("uid") Long userId);

    @Query("SELECT COUNT(pp) FROM ProgramProgress pp WHERE pp.user.id = :uid " +
           "AND pp.overallPct = 100")
    long countCompleted(@Param("uid") Long userId);

    /** For admin analytics: program engagement */
    @Query("SELECT pp.program.id, COUNT(pp), AVG(pp.overallPct) FROM ProgramProgress pp " +
           "GROUP BY pp.program.id ORDER BY COUNT(pp) DESC")
    List<Object[]> getProgramEngagementStats();
}
