package com.rediscoveru.repository;

import com.rediscoveru.entity.InAppNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {

    List<InAppNotification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<InAppNotification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);

    Optional<InAppNotification> findByIdAndUserId(Long id, Long userId);

    /** Mark all unread notifications for a user as read */
    @Modifying
    @Query("UPDATE InAppNotification n SET n.read = true, n.readAt = :now " +
           "WHERE n.user.id = :uid AND n.read = false")
    void markAllRead(@Param("uid") Long userId, @Param("now") LocalDateTime now);

    /** Clean up old read notifications (keep 30 days) */
    @Modifying
    @Query("DELETE FROM InAppNotification n WHERE n.read = true AND n.createdAt < :cutoff")
    void deleteOldRead(@Param("cutoff") LocalDateTime cutoff);
}
