package com.rediscoveru.controller;

import com.rediscoveru.dto.BookmarkRequest;
import com.rediscoveru.dto.NoteRequest;
import com.rediscoveru.service.NotesBookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Notes & Bookmarks Controller
 *
 * Notes:     /api/notes/**
 * Bookmarks: /api/bookmarks/**
 *
 * All endpoints require JWT authentication (PAID users only — enforced
 * at payment layer; these endpoints trust the JWT is valid).
 */
@RestController
@RequiredArgsConstructor
public class NotesBookmarkController {

    private final NotesBookmarkService service;

    // ════════════════ NOTES ══════════════════════════════════════════

    /** POST /api/notes — create a new note */
    @PostMapping("/api/notes")
    public ResponseEntity<?> createNote(
            @RequestBody NoteRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(service.createNote(ud.getUsername(), req)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** PUT /api/notes/{id} — edit an existing note */
    @PutMapping("/api/notes/{id}")
    public ResponseEntity<?> updateNote(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(service.updateNote(ud.getUsername(), id, body.get("noteText"))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** DELETE /api/notes/{id} — delete a note */
    @DeleteMapping("/api/notes/{id}")
    public ResponseEntity<?> deleteNote(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        try { service.deleteNote(ud.getUsername(), id);
              return ResponseEntity.ok(Map.of("deleted", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** GET /api/notes — all notes for the user */
    @GetMapping("/api/notes")
    public ResponseEntity<?> allNotes(@AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(service.getAllNotes(ud.getUsername())); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** GET /api/notes/program/{programId} — notes filtered by program */
    @GetMapping("/api/notes/program/{programId}")
    public ResponseEntity<?> notesByProgram(
            @PathVariable Long programId,
            @AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(service.getNotesByProgram(ud.getUsername(), programId)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** GET /api/notes/content?contentId=&contentType= — notes for a specific lesson */
    @GetMapping("/api/notes/content")
    public ResponseEntity<?> notesByContent(
            @RequestParam Long contentId,
            @RequestParam String contentType,
            @AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(service.getNotesByContent(ud.getUsername(), contentId, contentType)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ════════════════ BOOKMARKS ═══════════════════════════════════════

    /** POST /api/bookmarks/toggle — add or remove bookmark */
    @PostMapping("/api/bookmarks/toggle")
    public ResponseEntity<?> toggleBookmark(
            @RequestBody BookmarkRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(service.toggleBookmark(ud.getUsername(), req)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** DELETE /api/bookmarks/{id} — remove a specific bookmark by ID */
    @DeleteMapping("/api/bookmarks/{id}")
    public ResponseEntity<?> deleteBookmark(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        try { service.deleteBookmark(ud.getUsername(), id);
              return ResponseEntity.ok(Map.of("deleted", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** GET /api/bookmarks — all bookmarks */
    @GetMapping("/api/bookmarks")
    public ResponseEntity<?> allBookmarks(@AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(service.getAllBookmarks(ud.getUsername())); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** GET /api/bookmarks/program/{programId} */
    @GetMapping("/api/bookmarks/program/{programId}")
    public ResponseEntity<?> bookmarksByProgram(
            @PathVariable Long programId,
            @AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(service.getBookmarksByProgram(ud.getUsername(), programId)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** GET /api/bookmarks/content?contentId=&contentType= */
    @GetMapping("/api/bookmarks/content")
    public ResponseEntity<?> bookmarksByContent(
            @RequestParam Long contentId,
            @RequestParam String contentType,
            @AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(service.getBookmarksByContent(ud.getUsername(), contentId, contentType)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** GET /api/bookmarks/check?contentId=&contentType= — is this lesson bookmarked? */
    @GetMapping("/api/bookmarks/check")
    public ResponseEntity<?> isBookmarked(
            @RequestParam Long contentId,
            @RequestParam String contentType,
            @AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(Map.of("bookmarked",
            service.isBookmarked(ud.getUsername(), contentId, contentType))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
}
