package com.rediscoveru.repository;

import com.rediscoveru.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /** Admin: all announcements newest first */
    List<Announcement> findAllByOrderByDisplayOrderAscCreatedAtDesc();

    /** User: active and not expired */
    List<Announcement> findByActiveTrueAndExpiresAtAfterOrderByDisplayOrderAscCreatedAtDesc(
            LocalDateTime now);

    /** Scheduled cleanup */
    @Modifying
    @Query("DELETE FROM Announcement a WHERE a.expiresAt < :cutoff")
    void deleteByExpiresAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
