package com.rediscoveru.service;

import com.rediscoveru.entity.*;
import com.rediscoveru.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GamificationService {

    private final UserStreakRepository  streakRepo;
    private final BadgeRepository       badgeRepo;
    private final UserBadgeRepository   userBadgeRepo;
    private final UserRepository        userRepo;
    private final LessonProgressRepository lessonProgressRepo;
    private final ProgramProgressRepository programProgressRepo;
    private final NotificationService   notifService;

    // ════════════════════════ STREAKS ════════════════════════════════

    /**
     * Called every time a user accesses or completes a lesson.
     * Updates the streak if today is a new day.
     */
    @Transactional
    public UserStreak recordActivity(String email) {
        User user = resolveUser(email);
        LocalDate today = LocalDate.now();

        UserStreak streak = streakRepo.findByUserId(user.getId()).orElseGet(() -> {
            UserStreak s = new UserStreak();
            s.setUser(user);
            return s;
        });

        LocalDate last = streak.getLastActivityDate();

        if (last == null) {
            // First ever activity
            streak.setCurrentStreak(1);
            streak.setTotalActiveDays(1);
        } else if (today.equals(last)) {
            // Already active today — no change
            return streak;
        } else if (today.equals(last.plusDays(1))) {
            // Consecutive day — increment
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
            streak.setTotalActiveDays(streak.getTotalActiveDays() + 1);
        } else {
            // Missed at least one day — reset streak
            streak.setCurrentStreak(1);
            streak.setTotalActiveDays(streak.getTotalActiveDays() + 1);
        }

        // Update longest streak
        if (streak.getCurrentStreak() > streak.getLongestStreak()) {
            streak.setLongestStreak(streak.getCurrentStreak());
        }

        streak.setLastActivityDate(today);
        streak.setUpdatedAt(LocalDateTime.now());
        UserStreak saved = streakRepo.save(streak);

        // Check streak-based badges
        checkStreakBadges(user, saved.getCurrentStreak());

        return saved;
    }

    // ════════════════════════ BADGES ════════════════════════════════

    /**
     * Evaluate ALL badge conditions for a user.
     * Called after any significant action: lesson complete, note created, etc.
     */
    @Transactional
    public List<UserBadge> evaluateBadges(String email) {
        User user = resolveUser(email);
        List<Long> alreadyEarned = userBadgeRepo.findEarnedBadgeIds(user.getId());
        List<Badge> allBadges    = badgeRepo.findAll();
        List<UserBadge> newlyEarned = new ArrayList<>();

        long lessonsCompleted  = lessonProgressRepo.countByUserIdAndStatus(
            user.getId(), LessonProgress.ProgressStatus.COMPLETED);
        long programsCompleted = programProgressRepo.countCompleted(user.getId());
        int  currentStreak     = streakRepo.findByUserId(user.getId())
            .map(UserStreak::getCurrentStreak).orElse(0);

        for (Badge badge : allBadges) {
            if (alreadyEarned.contains(badge.getId())) continue; // already awarded
            boolean shouldAward = false;

            switch (badge.getBadgeType()) {
                case FIRST_LESSON       -> shouldAward = lessonsCompleted >= 1;
                case LESSONS_COMPLETED  -> shouldAward = lessonsCompleted >= badge.getThresholdValue();
                case PROGRAM_COMPLETE   -> shouldAward = programsCompleted >= badge.getThresholdValue();
                case STREAK_DAYS        -> shouldAward = currentStreak >= badge.getThresholdValue();
                default -> { /* handled elsewhere */ }
            }

            if (shouldAward) {
                UserBadge ub = awardBadge(user, badge);
                newlyEarned.add(ub);
            }
        }
        return newlyEarned;
    }

    @Transactional
    public UserBadge awardBadge(User user, Badge badge) {
        if (userBadgeRepo.existsByUserIdAndBadgeId(user.getId(), badge.getId())) {
            return null; // idempotent
        }
        UserBadge ub = new UserBadge();
        ub.setUser(user);
        ub.setBadge(badge);
        ub.setEarnedAt(LocalDateTime.now());
        UserBadge saved = userBadgeRepo.save(ub);
        // Send in-app notification
        notifService.onBadgeEarned(user, badge.getName());
        return saved;
    }

    // ── Streak badge check ────────────────────────────────────────────
    private void checkStreakBadges(User user, int currentStreak) {
        List<Badge> streakBadges = badgeRepo.findByBadgeType(Badge.BadgeType.STREAK_DAYS);
        for (Badge b : streakBadges) {
            if (currentStreak >= b.getThresholdValue()
                    && !userBadgeRepo.existsByUserIdAndBadgeId(user.getId(), b.getId())) {
                awardBadge(user, b);
            }
        }
    }

    // ════════════════════════ QUERIES ═══════════════════════════════

    public UserStreak getStreak(String email) {
        User user = resolveUser(email);
        return streakRepo.findByUserId(user.getId()).orElseGet(() -> {
            UserStreak s = new UserStreak(); s.setCurrentStreak(0); return s;
        });
    }

    public List<UserBadge> getUserBadges(String email) {
        return userBadgeRepo.findByUserIdOrderByEarnedAtDesc(resolveUser(email).getId());
    }

    public List<Badge> getAllBadges() {
        return badgeRepo.findAll();
    }

    public Map<String, Object> getAchievements(String email) {
        User user = resolveUser(email);
        List<UserBadge> earned = userBadgeRepo.findByUserIdOrderByEarnedAtDesc(user.getId());
        List<Badge> all        = badgeRepo.findAll();
        List<Long>  earnedIds  = userBadgeRepo.findEarnedBadgeIds(user.getId());
        UserStreak  streak     = streakRepo.findByUserId(user.getId())
            .orElse(new UserStreak());

        long lessonsCompleted  = lessonProgressRepo.countByUserIdAndStatus(
            user.getId(), LessonProgress.ProgressStatus.COMPLETED);
        long programsCompleted = programProgressRepo.countCompleted(user.getId());

        return Map.of(
            "currentStreak",    streak.getCurrentStreak(),
            "longestStreak",    streak.getLongestStreak(),
            "totalActiveDays",  streak.getTotalActiveDays(),
            "totalBadges",      earned.size(),
            "totalBadgesAvailable", all.size(),
            "badges",           earned,
            "lessonsCompleted", lessonsCompleted,
            "programsCompleted",programsCompleted
        );
    }

    /** Streak leaderboard — top 10 users */
    public List<Map<String, Object>> getLeaderboard() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserStreak s : streakRepo.findLeaderboard(PageRequest.of(0, 10))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name",          s.getUser().getName());
            m.put("currentStreak", s.getCurrentStreak());
            m.put("longestStreak", s.getLongestStreak());
            m.put("totalActiveDays", s.getTotalActiveDays());
            result.add(m);
        }
        return result;
    }

    private User resolveUser(String email) {
        return userRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}
