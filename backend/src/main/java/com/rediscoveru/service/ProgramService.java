package com.rediscoveru.service;

import com.rediscoveru.entity.*;
import com.rediscoveru.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service @RequiredArgsConstructor
public class ProgramService {

    private final ProgramRepository          programRepo;
    private final ProgramContentRepository   contentRepo;
    private final LiveSessionRepository      sessionRepo;
    private final ProgramCategoryRepository  categoryRepo;
    private final UploadedFileRepository     fileRepo;

    // ── Programs ──────────────────────────────────────────────────
    public List<Program> getActivePrograms() {
        return programRepo.findByActiveTrueOrderByCreatedAtDesc();
    }

    public List<Program> getAllPrograms() {
        return programRepo.findAllByOrderByCreatedAtDesc();
    }

    public Program getById(Long id) {
        return programRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Program not found: " + id));
    }

    public Program create(Program p) {
        if (p.getTitle() == null || p.getTitle().isBlank())
            throw new RuntimeException("Program title is required");
        // Resolve category if provided by ID
        if (p.getCategory() != null && p.getCategory().getId() != null) {
            categoryRepo.findById(p.getCategory().getId()).ifPresent(p::setCategory);
        }
        return programRepo.save(p);
    }

    public Program update(Long id, Program updated) {
        Program existing = getById(id);
        if (updated.getTitle() != null)       existing.setTitle(updated.getTitle());
        if (updated.getDescription() != null) existing.setDescription(updated.getDescription());
        if (updated.getType() != null)        existing.setType(updated.getType());
        if (updated.getDuration() != null)    existing.setDuration(updated.getDuration());
        if (updated.getTagline() != null)     existing.setTagline(updated.getTagline());
        if (updated.getCoverImageUrl() != null) existing.setCoverImageUrl(updated.getCoverImageUrl());
        existing.setActive(updated.isActive());
        // Category update
        if (updated.getCategory() != null && updated.getCategory().getId() != null) {
            categoryRepo.findById(updated.getCategory().getId()).ifPresent(existing::setCategory);
        } else if (updated.getCategory() == null) {
            existing.setCategory(null);
        }
        return programRepo.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        contentRepo.deleteByProgramId(id);
        fileRepo.deleteByProgramId(id);
        sessionRepo.deleteByProgramId(id);
        programRepo.deleteById(id);
    }

    public void toggleActive(Long id) {
        Program p = getById(id);
        p.setActive(!p.isActive());
        programRepo.save(p);
    }

    // ── Content ───────────────────────────────────────────────────
    public ProgramContent addContent(Long programId, ProgramContent content) {
        content.setProgram(getById(programId));
        return contentRepo.save(content);
    }

    public ProgramContent updateContent(Long contentId, ProgramContent updated) {
        ProgramContent existing = contentRepo.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Content not found: " + contentId));
        if (updated.getTitle() != null)       existing.setTitle(updated.getTitle());
        if (updated.getContentType() != null) existing.setContentType(updated.getContentType());
        if (updated.getContentUrl() != null)  existing.setContentUrl(updated.getContentUrl());
        if (updated.getOrderIndex() != null)  existing.setOrderIndex(updated.getOrderIndex());
        existing.setScheduleText(updated.getScheduleText());
        return contentRepo.save(existing);
    }

    public void deleteContent(Long contentId) { contentRepo.deleteById(contentId); }

    public List<ProgramContent> getContent(Long programId) {
        return contentRepo.findByProgramIdOrderByOrderIndexAsc(programId);
    }

    // ── Live Sessions ─────────────────────────────────────────────
    public LiveSession addSession(Long programId, LiveSession session) {
        session.setProgram(getById(programId));
        return sessionRepo.save(session);
    }

    public LiveSession updateSession(Long sessionId, LiveSession updated) {
        LiveSession existing = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        if (updated.getTitle() != null)           existing.setTitle(updated.getTitle());
        if (updated.getMeetingLink() != null)      existing.setMeetingLink(updated.getMeetingLink());
        if (updated.getRecurrenceType() != null)   existing.setRecurrenceType(updated.getRecurrenceType());
        if (updated.getStartTime() != null)        existing.setStartTime(updated.getStartTime());
        if (updated.getEndTime() != null)          existing.setEndTime(updated.getEndTime());
        if (updated.getTimezone() != null)         existing.setTimezone(updated.getTimezone());
        if (updated.getRecurrenceDays() != null)   existing.setRecurrenceDays(updated.getRecurrenceDays());
        if (updated.getValidFrom() != null)        existing.setValidFrom(updated.getValidFrom());
        if (updated.getValidTo() != null)          existing.setValidTo(updated.getValidTo());
        if (updated.getSpecificDate() != null)     existing.setSpecificDate(updated.getSpecificDate());
        existing.setActive(updated.isActive());
        return sessionRepo.save(existing);
    }

    public void deleteSession(Long sessionId) { sessionRepo.deleteById(sessionId); }

    public void toggleSession(Long sessionId) {
        LiveSession s = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        s.setActive(!s.isActive());
        sessionRepo.save(s);
    }

    public List<LiveSession> getSessions(Long programId) {
        return sessionRepo.findByProgramIdOrderByCreatedAtAsc(programId);
    }

    /** Unified sessions list for user dashboard. */
    public List<Map<String, Object>> getAllActiveSessions() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (LiveSession s : sessionRepo.findAllActiveSessions()) {
            result.add(sessionToMap(s));
        }
        // Also include MEETING content items
        for (ProgramContent c : contentRepo
                .findByContentTypeAndProgramActiveTrueOrderByProgramIdAscOrderIndexAsc(
                        ProgramContent.ContentType.MEETING)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",           "mc-" + c.getId());
            m.put("title",        c.getTitle());
            m.put("meetingLink",  c.getContentUrl());
            m.put("scheduleText", c.getScheduleText());
            m.put("programTitle", c.getProgram() != null ? c.getProgram().getTitle() : "");
            m.put("type",         "MEETING_CONTENT");
            result.add(m);
        }
        return result;
    }

    public Map<String, Object> getProgramDetails(Long id) {
        Program program  = getById(id);
        List<ProgramContent> contents = contentRepo.findByProgramIdOrderByOrderIndexAsc(id);
        List<LiveSession>    sessions = sessionRepo.findByProgramIdOrderByCreatedAtAsc(id);
        List<UploadedFile>   files    = fileRepo.findByProgramIdOrderByOrderIndexAscUploadedAtAsc(id);

        List<Map<String, Object>> allSessions = new ArrayList<>();
        for (LiveSession s : sessions) allSessions.add(sessionToMap(s));

        for (ProgramContent c : contents) {
            if (c.getContentType() == ProgramContent.ContentType.MEETING) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", "mc-" + c.getId());
                m.put("title", c.getTitle());
                m.put("meetingLink", c.getContentUrl());
                m.put("scheduleText", c.getScheduleText());
                m.put("type", "MEETING_CONTENT");
                allSessions.add(m);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("program",      program);
        result.put("contents",     contents);
        result.put("files",        files);
        result.put("liveSessions", allSessions);
        return result;
    }

    private Map<String, Object> sessionToMap(LiveSession s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",             s.getId());
        m.put("title",          s.getTitle());
        m.put("meetingLink",    s.getMeetingLink());
        m.put("recurrenceType", s.getRecurrenceType());
        m.put("recurrenceDays", s.getRecurrenceDays());
        m.put("startTime",      s.getStartTime() != null ? s.getStartTime().toString() : null);
        m.put("endTime",        s.getEndTime() != null ? s.getEndTime().toString() : null);
        m.put("timezone",       s.getTimezone());
        m.put("validFrom",      s.getValidFrom() != null ? s.getValidFrom().toString() : null);
        m.put("validTo",        s.getValidTo() != null ? s.getValidTo().toString() : null);
        m.put("specificDate",   s.getSpecificDate() != null ? s.getSpecificDate().toString() : null);
        m.put("scheduleText",   buildScheduleText(s));
        m.put("programTitle",   s.getProgram() != null ? s.getProgram().getTitle() : "");
        m.put("type",           "LIVE_SESSION");
        return m;
    }

    public String buildScheduleText(LiveSession s) {
        StringBuilder sb = new StringBuilder();
        switch (s.getRecurrenceType()) {
            case DAILY               -> sb.append("Every Day");
            case WEEKDAYS            -> sb.append("Mon – Fri");
            case CUSTOM_DAYS         -> sb.append(s.getRecurrenceDays() != null
                                             ? s.getRecurrenceDays().replace(",", " · ") : "Custom Days");
            case SPECIFIC_DATE_RANGE -> {
                if (s.getValidFrom() != null) sb.append(s.getValidFrom());
                if (s.getValidTo() != null) sb.append(" to ").append(s.getValidTo());
            }
            default -> sb.append(s.getSpecificDate() != null ? s.getSpecificDate().toString() : "One-time");
        }
        if (s.getStartTime() != null) {
            sb.append(" · ").append(s.getStartTime());
            if (s.getEndTime() != null) sb.append(" – ").append(s.getEndTime());
            sb.append(" ").append(s.getTimezone() != null ? s.getTimezone().replace("Asia/Kolkata", "IST") : "IST");
        }
        return sb.toString();
    }
}
