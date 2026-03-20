package com.rediscoveru.repository;

import com.rediscoveru.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /** Admin view — all announcements, ordered by displayOrder then newest first */
    List<Announcement> findAllByOrderByDisplayOrderAscCreatedAtDesc();

    /** User view — active AND not expired, ordered by displayOrder then newest */
    List<Announcement> findByActiveTrueAndExpiresAtAfterOrderByDisplayOrderAscCreatedAtDesc(
        LocalDateTime now);

    /** Scheduled cleanup — find expired rows to delete */
    List<Announcement> findByExpiresAtBefore(LocalDateTime cutoff);

    /** Bulk delete expired announcements */
    @Modifying
    @Query("DELETE FROM Announcement a WHERE a.expiresAt < :cutoff")
    void deleteByExpiresAtBefore(@Param("cutoff") LocalDateTime cutoff);

    /** Reorder — update displayOrder for a single announcement */
    @Modifying
    @Query("UPDATE Announcement a SET a.displayOrder = :order WHERE a.id = :id")
    void updateDisplayOrder(@Param("id") Long id, @Param("order") int order);
}
