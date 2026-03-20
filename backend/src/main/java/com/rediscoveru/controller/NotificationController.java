package com.rediscoveru.controller;

import com.rediscoveru.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * In-App Notifications Controller
 * Base path: /api/notifications
 *
 * Email notifications are handled by EmailNotificationService (separate).
 * This controller serves the notification bell in the user dashboard.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notifService;

    /** GET /api/notifications?limit=20 — paginated recent notifications */
    @GetMapping
    public ResponseEntity<?> getNotifications(
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(notifService.getRecentNotifications(ud.getUsername(), limit)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** GET /api/notifications/unread — all unread notifications */
    @GetMapping("/unread")
    public ResponseEntity<?> unread(@AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(notifService.getUnreadNotifications(ud.getUsername())); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** GET /api/notifications/count — unread count (for badge) */
    @GetMapping("/count")
    public ResponseEntity<?> unreadCount(@AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(Map.of("count",
            notifService.getUnreadCount(ud.getUsername()))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** GET /api/notifications/summary — unread count + 5 recent (single call for navbar) */
    @GetMapping("/summary")
    public ResponseEntity<?> summary(@AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(notifService.getNotificationSummary(ud.getUsername())); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** PATCH /api/notifications/{id}/read — mark one as read */
    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        try { notifService.markAsRead(ud.getUsername(), id);
              return ResponseEntity.ok(Map.of("ok", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** PATCH /api/notifications/read-all — mark all as read */
    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllRead(@AuthenticationPrincipal UserDetails ud) {
        try { notifService.markAllAsRead(ud.getUsername());
              return ResponseEntity.ok(Map.of("ok", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    /** DELETE /api/notifications/{id} — delete a notification */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        try { notifService.deleteNotification(ud.getUsername(), id);
              return ResponseEntity.ok(Map.of("deleted", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
}
