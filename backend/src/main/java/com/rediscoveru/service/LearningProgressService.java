package com.rediscoveru.service;

import com.rediscoveru.dto.LessonProgressRequest;
import com.rediscoveru.entity.*;
import com.rediscoveru.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LearningProgressService {

    private final LessonProgressRepository  lessonProgressRepo;
    private final ProgramProgressRepository programProgressRepo;
    private final UserRepository            userRepo;
    private final ProgramRepository         programRepo;
    private final ProgramContentRepository  contentRepo;
    private final UploadedFileRepository    fileRepo;
    private final GamificationService       gamificationService;

    // ── Record / update a lesson access ──────────────────────────────

    @Transactional
    public LessonProgress recordProgress(String email, LessonProgressRequest req) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Program program = programRepo.findById(req.getProgramId())
                .orElseThrow(() -> new RuntimeException("Program not found"));

        // Upsert: find existing or create
        LessonProgress lp = lessonProgressRepo
                .findByUserIdAndContentIdAndContentType(
                    user.getId(), req.getContentId(), req.getContentType())
                .orElseGet(() -> {
                    LessonProgress n = new LessonProgress();
                    n.setUser(user);
                    n.setProgram(program);
                    n.setContentId(req.getContentId());
                    n.setContentType(req.getContentType());
                    n.setContentTitle(req.getContentTitle());
                    return n;
                });

        // Update watch position for video content
        if (req.getWatchedSeconds() != null) {
            lp.setWatchedSeconds(req.getWatchedSeconds());
        }
        if (req.getTotalSeconds() != null && req.getTotalSeconds() > 0) {
            lp.setTotalSeconds(req.getTotalSeconds());
            // Auto-complete when ≥ 85% watched
            int pct = (int) ((req.getWatchedSeconds() * 100.0) / req.getTotalSeconds());
            lp.setCompletionPct(Math.min(pct, 100));
            if (pct >= 85 && lp.getStatus() != LessonProgress.ProgressStatus.COMPLETED) {
                lp.setStatus(LessonProgress.ProgressStatus.COMPLETED);
                lp.setCompletedAt(LocalDateTime.now());
            }
        }

        // Manual completion override
        if (Boolean.TRUE.equals(req.getMarkCompleted())) {
            lp.setStatus(LessonProgress.ProgressStatus.COMPLETED);
            lp.setCompletionPct(100);
            if (lp.getCompletedAt() == null) lp.setCompletedAt(LocalDateTime.now());
        }

        // Mark in-progress if started but not complete
        if (lp.getStatus() == LessonProgress.ProgressStatus.NOT_STARTED
                && (req.getWatchedSeconds() != null && req.getWatchedSeconds() > 0)) {
            lp.setStatus(LessonProgress.ProgressStatus.IN_PROGRESS);
        }

        lp.setLastAccessedAt(LocalDateTime.now());
        LessonProgress saved = lessonProgressRepo.save(lp);

        // Re-compute program-level progress
        recalculateProgramProgress(user, program);

        // ── Gamification hooks ─────────────────────────────────
        try {
            gamificationService.recordActivity(email);
            gamificationService.evaluateBadges(email);
        } catch (Exception ignored) { /* non-fatal */ }

        return saved;
    }

    // ── Recalculate program-level progress ───────────────────────────

    @Transactional
    public void recalculateProgramProgress(User user, Program program) {
        // Count total lessons: ProgramContent + UploadedFiles for program
        int totalContent = contentRepo.findByProgramIdOrderByOrderIndexAsc(program.getId()).size();
        int totalFiles   = fileRepo.findByProgramIdOrderByOrderIndexAscUploadedAtAsc(program.getId()).size();
        int total = totalContent + totalFiles;
        if (total == 0) return;

        long completed = lessonProgressRepo.countCompleted(user.getId(), program.getId());
        int pct = (int) ((completed * 100.0) / total);

        ProgramProgress pp = programProgressRepo
                .findByUserIdAndProgramId(user.getId(), program.getId())
                .orElseGet(() -> {
                    ProgramProgress n = new ProgramProgress();
                    n.setUser(user);
                    n.setProgram(program);
                    n.setStartedAt(LocalDateTime.now());
                    return n;
                });

        pp.setTotalLessons(total);
        pp.setCompletedLessons((int) completed);
        pp.setOverallPct(pct);
        pp.setUpdatedAt(LocalDateTime.now());

        if (pct >= 100 && pp.getCompletedAt() == null) {
            pp.setCompletedAt(LocalDateTime.now());
        }

        // Set last accessed for resume
        lessonProgressRepo
                .findRecentByUser(user.getId(), PageRequest.of(0, 1))
                .stream().findFirst().ifPresent(last -> {
                    if (last.getProgram().getId().equals(program.getId())) {
                        pp.setLastContentId(last.getContentId());
                        pp.setLastContentType(last.getContentType());
                        pp.setLastContentTitle(last.getContentTitle());
                    }
                });

        programProgressRepo.save(pp);
    }

    // ── Getters ───────────────────────────────────────────────────────

    public List<LessonProgress> getLessonProgressForProgram(String email, Long programId) {
        User user = userRepo.findByEmail(email).orElseThrow();
        return lessonProgressRepo.findByUserIdAndProgramId(user.getId(), programId);
    }

    public Optional<ProgramProgress> getProgramProgress(String email, Long programId) {
        User user = userRepo.findByEmail(email).orElseThrow();
        return programProgressRepo.findByUserIdAndProgramId(user.getId(), programId);
    }

    public List<ProgramProgress> getAllProgressForUser(String email) {
        User user = userRepo.findByEmail(email).orElseThrow();
        return programProgressRepo.findByUserIdOrderByUpdatedAtDesc(user.getId());
    }

    public List<ProgramProgress> getInProgressPrograms(String email) {
        User user = userRepo.findByEmail(email).orElseThrow();
        return programProgressRepo.findInProgressByUser(user.getId());
    }

    /** Last accessed lesson across all programs — "Resume" */
    public Optional<LessonProgress> getLastAccessedLesson(String email) {
        User user = userRepo.findByEmail(email).orElseThrow();
        return lessonProgressRepo
                .findRecentByUser(user.getId(), PageRequest.of(0, 1))
                .stream().findFirst();
    }
}
