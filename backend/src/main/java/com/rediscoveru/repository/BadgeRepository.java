package com.rediscoveru.repository;

import com.rediscoveru.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BadgeRepository extends JpaRepository<Badge, Long> {
    List<Badge> findByBadgeType(Badge.BadgeType type);
    Optional<Badge> findByName(String name);
    List<Badge> findByBadgeTypeAndThresholdValue(Badge.BadgeType type, int threshold);
}
