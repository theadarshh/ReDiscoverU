package com.rediscoveru.service;

import com.rediscoveru.entity.*;
import com.rediscoveru.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserRepository            userRepo;
    private final PaymentRepository         paymentRepo;
    private final ProgramRepository         programRepo;
    private final ProgramProgressRepository programProgressRepo;
    private final LessonProgressRepository  lessonProgressRepo;
    private final CommentRepository         commentRepo;
    private final UserBadgeRepository       userBadgeRepo;
    private final UserStreakRepository      streakRepo;
    private final InAppNotificationRepository notifRepo;

    // ── Overview stats ────────────────────────────────────────────────
    public Map<String, Object> getOverviewStats() {
        long totalUsers   = userRepo.count();
        long paidUsers    = userRepo.countBySubscriptionStatus(User.SubscriptionStatus.PAID);
        long pendingUsers = userRepo.countBySubscriptionStatus(User.SubscriptionStatus.PENDING);
        long activeUsers  = getActiveUsersCount(30); // active in last 30 days

        BigDecimal totalRevenue = paymentRepo.getTotalRevenue();
        long successfulPayments = paymentRepo.countSuccessful();

        long totalPrograms  = programRepo.count();
        long activePrograms = programRepo.countByActiveTrue();

        // Engagement
        long totalLessonsCompleted = lessonProgressRepo.countByStatus(
            LessonProgress.ProgressStatus.COMPLETED);
        long totalComments = commentRepo.count();
        long totalBadges   = userBadgeRepo.count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers",         totalUsers);
        result.put("paidUsers",          paidUsers);
        result.put("pendingUsers",        pendingUsers);
        result.put("activeUsers30Days",  activeUsers);
        result.put("totalRevenue",       totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        result.put("successfulPayments", successfulPayments);
        result.put("totalPrograms",      totalPrograms);
        result.put("activePrograms",     activePrograms);
        result.put("totalLessonsCompleted", totalLessonsCompleted);
        result.put("totalComments",      totalComments);
        result.put("totalBadgesAwarded", totalBadges);
        return result;
    }

    // ── User stats ────────────────────────────────────────────────────
    public Map<String, Object> getUserStats() {
        // New users by month (last 6 months)
        List<Map<String, Object>> newUsersByMonth = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime from = LocalDateTime.now().minusMonths(i).withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0);
            LocalDateTime to   = from.plusMonths(1);
            long count = userRepo.countByCreatedAtBetween(from, to);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", from.getMonth().toString());
            m.put("year",  from.getYear());
            m.put("count", count);
            newUsersByMonth.add(m);
        }

        // New paid users by month
        List<Map<String, Object>> paidByMonth = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime from = LocalDateTime.now().minusMonths(i).withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0);
            LocalDateTime to = from.plusMonths(1);
            long count = paymentRepo.countByCreatedAtBetweenAndStatus(from, to);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", from.getMonth().toString());
            m.put("year",  from.getYear());
            m.put("count", count);
            paidByMonth.add(m);
        }

        return Map.of(
            "newUsersByMonth", newUsersByMonth,
            "conversionRate",  calculateConversionRate(),
            "paidByMonth",     paidByMonth
        );
    }

    // ── Revenue stats ─────────────────────────────────────────────────
    public Map<String, Object> getRevenueStats() {
        List<Map<String, Object>> revenueByMonth = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime from = LocalDateTime.now().minusMonths(i).withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0);
            LocalDateTime to = from.plusMonths(1);
            BigDecimal revenue = paymentRepo.sumRevenueByMonth(from, to);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month",   from.getMonth().toString());
            m.put("year",    from.getYear());
            m.put("revenue", revenue != null ? revenue : BigDecimal.ZERO);
            revenueByMonth.add(m);
        }
        return Map.of(
            "totalRevenue",    paymentRepo.getTotalRevenue(),
            "revenueByMonth",  revenueByMonth,
            "avgOrderValue",   calculateAvgOrderValue()
        );
    }

    // ── Program engagement ────────────────────────────────────────────
    public List<Map<String, Object>> getProgramEngagement() {
        List<Object[]> rows = programProgressRepo.getProgramEngagementStats();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long programId   = (Long)   row[0];
            Long userCount   = (Long)   row[1];
            Double avgPct    = (Double) row[2];

            programRepo.findById(programId).ifPresent(prog -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("programId",    programId);
                m.put("programTitle", prog.getTitle());
                m.put("usersEnrolled", userCount);
                m.put("avgCompletion", avgPct != null ? Math.round(avgPct) : 0);
                long comments = commentRepo.countByProgramIdAndDeletedFalse(programId);
                m.put("commentCount", comments);
                result.add(m);
            });
        }
        return result;
    }

    // ── Top learners ──────────────────────────────────────────────────
    public List<Map<String, Object>> getTopLearners() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserStreak s : streakRepo.findLeaderboard(PageRequest.of(0, 10))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId",        s.getUser().getId());
            m.put("name",          s.getUser().getName());
            m.put("email",         s.getUser().getEmail());
            m.put("currentStreak", s.getCurrentStreak());
            m.put("longestStreak", s.getLongestStreak());
            long lessons = lessonProgressRepo.countByUserIdAndStatus(
                s.getUser().getId(), LessonProgress.ProgressStatus.COMPLETED);
            long badges  = userBadgeRepo.countByUserId(s.getUser().getId());
            m.put("lessonsCompleted", lessons);
            m.put("badgesEarned",     badges);
            result.add(m);
        }
        return result;
    }

    // ── Recent activity feed ──────────────────────────────────────────
    public Map<String, Object> getActivityFeed() {
        // Recent payments
        List<Map<String, Object>> recentPayments = new ArrayList<>();
        paymentRepo.findAllByOrderByCreatedAtDesc().stream().limit(10).forEach(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type",     "PAYMENT");
            m.put("userName", p.getUser() != null ? p.getUser().getName() : "Unknown");
            m.put("amount",   p.getFinalAmount());
            m.put("status",   p.getPaymentStatus());
            m.put("date",     p.getCreatedAt());
            recentPayments.add(m);
        });
        return Map.of("recentPayments", recentPayments);
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private long getActiveUsersCount(int days) {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            return lessonProgressRepo.countDistinctUsersSince(since);
        } catch (Exception e) { return 0; }
    }

    private double calculateConversionRate() {
        long total = userRepo.count();
        long paid  = userRepo.countBySubscriptionStatus(User.SubscriptionStatus.PAID);
        return total > 0 ? Math.round((paid * 1000.0 / total)) / 10.0 : 0.0;
    }

    private BigDecimal calculateAvgOrderValue() {
        try {
            BigDecimal total  = paymentRepo.getTotalRevenue();
            long count = paymentRepo.countSuccessful();
            if (total == null || count == 0) return BigDecimal.ZERO;
            return total.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP);
        } catch (Exception e) { return BigDecimal.ZERO; }
    }
}
