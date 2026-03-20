package com.rediscoveru.controller;

import com.rediscoveru.dto.CommentRequest;
import com.rediscoveru.service.DiscussionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Discussion / Community Controller
 *
 * User endpoints:  /api/discussions/**
 * Admin endpoints: /api/admin/discussions/**  (separate mapping — admin must have ROLE_ADMIN)
 */
@RestController
@RequiredArgsConstructor
public class DiscussionController {

    private final DiscussionService discussionService;

    // ── User endpoints ────────────────────────────────────────────

    /** POST /api/discussions — post a comment or reply */
    @PostMapping("/api/discussions")
    public ResponseEntity<?> postComment(
            @RequestBody CommentRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(discussionService.postComment(ud.getUsername(), req)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /**
     * GET /api/discussions?programId=&contentId=&contentType=
     * Full comment thread (top-level comments + replies) for a content item.
     */
    @GetMapping("/api/discussions")
    public ResponseEntity<?> getThread(
            @RequestParam Long programId,
            @RequestParam Long contentId,
            @RequestParam String contentType,
            @AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(
            discussionService.getCommentThread(ud.getUsername(), programId, contentId, contentType));
        }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** PUT /api/discussions/{id} — edit own comment */
    @PutMapping("/api/discussions/{id}")
    public ResponseEntity<?> editComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(
            discussionService.editComment(ud.getUsername(), id, body.get("commentText")));
        }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** DELETE /api/discussions/{id} — soft-delete own comment */
    @DeleteMapping("/api/discussions/{id}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        try { discussionService.deleteComment(ud.getUsername(), id, false);
              return ResponseEntity.ok(Map.of("deleted", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** POST /api/discussions/{id}/like — toggle like on a comment */
    @PostMapping("/api/discussions/{id}/like")
    public ResponseEntity<?> toggleLike(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(discussionService.toggleLike(ud.getUsername(), id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── Admin endpoint ────────────────────────────────────────────

    /** DELETE /api/admin/discussions/{id} — admin can force-delete any comment */
    @DeleteMapping("/api/admin/discussions/{id}")
    public ResponseEntity<?> adminDeleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        try { discussionService.deleteComment(ud.getUsername(), id, true);
              return ResponseEntity.ok(Map.of("deleted", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
}
