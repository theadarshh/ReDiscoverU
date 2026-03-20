package com.rediscoveru.repository;

import com.rediscoveru.entity.UserBadge;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    List<UserBadge> findByUserIdOrderByEarnedAtDesc(Long userId);

    List<UserBadge> findByUserIdOrderByEarnedAtDesc(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    boolean existsByUserIdAndBadgeId(Long userId, Long badgeId);

    @Query("SELECT ub.badge.id FROM UserBadge ub WHERE ub.user.id = :uid")
    List<Long> findEarnedBadgeIds(@Param("uid") Long userId);
}
