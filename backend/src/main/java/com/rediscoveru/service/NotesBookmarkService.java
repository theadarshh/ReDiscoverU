package com.rediscoveru.service;

import com.rediscoveru.dto.BookmarkRequest;
import com.rediscoveru.dto.NoteRequest;
import com.rediscoveru.entity.*;
import com.rediscoveru.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotesBookmarkService {

    private final UserNoteRepository noteRepo;
    private final BookmarkRepository bookmarkRepo;
    private final UserRepository     userRepo;
    private final ProgramRepository  programRepo;

    // ════════════════════════ NOTES ═════════════════════════════════

    @Transactional
    public UserNote createNote(String email, NoteRequest req) {
        User    user    = resolveUser(email);
        Program program = resolveProgram(req.getProgramId());

        if (req.getNoteText() == null || req.getNoteText().isBlank())
            throw new RuntimeException("Note text cannot be empty");

        UserNote note = new UserNote();
        note.setUser(user);
        note.setProgram(program);
        note.setContentId(req.getContentId());
        note.setContentType(req.getContentType());
        note.setContentTitle(req.getContentTitle());
        note.setNoteText(req.getNoteText().trim());
        note.setTimestampSeconds(req.getTimestampSeconds());
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        return noteRepo.save(note);
    }

    @Transactional
    public UserNote updateNote(String email, Long noteId, String newText) {
        User user = resolveUser(email);
        UserNote note = noteRepo.findByIdAndUserId(noteId, user.getId())
                .orElseThrow(() -> new RuntimeException("Note not found or does not belong to you"));
        if (newText == null || newText.isBlank())
            throw new RuntimeException("Note text cannot be empty");
        note.setNoteText(newText.trim());
        note.setUpdatedAt(LocalDateTime.now());
        return noteRepo.save(note);
    }

    @Transactional
    public void deleteNote(String email, Long noteId) {
        User user = resolveUser(email);
        UserNote note = noteRepo.findByIdAndUserId(noteId, user.getId())
                .orElseThrow(() -> new RuntimeException("Note not found or does not belong to you"));
        noteRepo.delete(note);
    }

    public List<UserNote> getAllNotes(String email) {
        return noteRepo.findByUserIdOrderByUpdatedAtDesc(resolveUser(email).getId());
    }

    public List<UserNote> getNotesByProgram(String email, Long programId) {
        return noteRepo.findByUserIdAndProgramIdOrderByUpdatedAtDesc(
            resolveUser(email).getId(), programId);
    }

    public List<UserNote> getNotesByContent(String email, Long contentId, String contentType) {
        return noteRepo.findByUserIdAndContentIdAndContentTypeOrderByCreatedAtAsc(
            resolveUser(email).getId(), contentId, contentType);
    }

    // ════════════════════════ BOOKMARKS ═════════════════════════════

    @Transactional
    public Map<String, Object> toggleBookmark(String email, BookmarkRequest req) {
        User user = resolveUser(email);

        // For content-level bookmarks (no timestamp) — toggle
        if (req.getTimestampSeconds() == null) {
            boolean exists = bookmarkRepo.existsByUserIdAndContentIdAndContentTypeAndTimestampSecondsIsNull(
                user.getId(), req.getContentId(), req.getContentType());
            if (exists) {
                bookmarkRepo.findContentBookmark(user.getId(), req.getContentId(), req.getContentType())
                    .ifPresent(bookmarkRepo::delete);
                return Map.of("bookmarked", false, "message", "Bookmark removed");
            }
        }

        Program program = resolveProgram(req.getProgramId());
        Bookmark bm = new Bookmark();
        bm.setUser(user);
        bm.setProgram(program);
        bm.setContentId(req.getContentId());
        bm.setContentType(req.getContentType());
        bm.setContentTitle(req.getContentTitle());
        bm.setTimestampSeconds(req.getTimestampSeconds());
        bm.setLabel(req.getLabel());
        bm.setCreatedAt(LocalDateTime.now());
        Bookmark saved = bookmarkRepo.save(bm);
        return Map.of("bookmarked", true, "bookmark", saved, "message", "Bookmarked");
    }

    @Transactional
    public void deleteBookmark(String email, Long bookmarkId) {
        User user = resolveUser(email);
        Bookmark bm = bookmarkRepo.findByIdAndUserId(bookmarkId, user.getId())
                .orElseThrow(() -> new RuntimeException("Bookmark not found or does not belong to you"));
        bookmarkRepo.delete(bm);
    }

    public List<Bookmark> getAllBookmarks(String email) {
        return bookmarkRepo.findByUserIdOrderByCreatedAtDesc(resolveUser(email).getId());
    }

    public List<Bookmark> getBookmarksByProgram(String email, Long programId) {
        return bookmarkRepo.findByUserIdAndProgramIdOrderByCreatedAtDesc(
            resolveUser(email).getId(), programId);
    }

    public List<Bookmark> getBookmarksByContent(String email, Long contentId, String contentType) {
        return bookmarkRepo.findByUserIdAndContentIdAndContentTypeOrderByTimestampSecondsAsc(
            resolveUser(email).getId(), contentId, contentType);
    }

    public boolean isBookmarked(String email, Long contentId, String contentType) {
        return bookmarkRepo.existsByUserIdAndContentIdAndContentTypeAndTimestampSecondsIsNull(
            resolveUser(email).getId(), contentId, contentType);
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private User resolveUser(String email) {
        return userRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
    private Program resolveProgram(Long id) {
        return programRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Program not found: " + id));
    }
}
