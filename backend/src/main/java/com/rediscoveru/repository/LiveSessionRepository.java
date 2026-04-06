package com.rediscoveru.repository;

import com.rediscoveru.entity.LiveSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface LiveSessionRepository extends JpaRepository<LiveSession, Long> {

    List<LiveSession> findByProgramIdOrderByCreatedAtAsc(Long programId);

    @Query("SELECT s FROM LiveSession s WHERE s.active = true ORDER BY s.createdAt ASC")
    List<LiveSession> findAllActiveSessions();

    @Modifying
    @Query("DELETE FROM LiveSession s WHERE s.program.id = :programId")
    void deleteByProgramId(Long programId);
}
