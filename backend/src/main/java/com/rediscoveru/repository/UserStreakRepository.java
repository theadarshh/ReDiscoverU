package com.rediscoveru.repository;

import com.rediscoveru.entity.UserStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserStreakRepository extends JpaRepository<UserStreak, Long> {
    Optional<UserStreak> findByUserId(Long userId);

    @Query("SELECT us FROM UserStreak us ORDER BY us.currentStreak DESC")
    List<UserStreak> findLeaderboard(org.springframework.data.domain.Pageable pageable);
}
