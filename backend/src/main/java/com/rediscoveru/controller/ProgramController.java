package com.rediscoveru.controller;

import com.rediscoveru.entity.*;
import com.rediscoveru.repository.UserRepository;
import com.rediscoveru.repository.WhatsAppGroupRepository;
import com.rediscoveru.service.CategoryService;
import com.rediscoveru.service.FileUploadService;
import com.rediscoveru.service.PaymentService;
import com.rediscoveru.service.ProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService          programService;
    private final PaymentService          paymentService;
    private final CategoryService         categoryService;
    private final FileUploadService       fileUploadService;
    private final UserRepository          userRepo;
    private final WhatsAppGroupRepository waGroupRepo;

    // ── Public ─────────────────────────────────────────────────────
    @GetMapping("/programs/public")
    public ResponseEntity<?> publicPrograms() {
        try { return ResponseEntity.ok(programService.getActivePrograms()); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @GetMapping("/programs/categories")
    public ResponseEntity<?> publicCategories() {
        try { return ResponseEntity.ok(categoryService.getActive()); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    // ── Paid users ─────────────────────────────────────────────────
    @GetMapping("/user/programs")
    public ResponseEntity<?> allPrograms(@AuthenticationPrincipal UserDetails ud) {
        if (ud == null) return ResponseEntity.status(401).body(Map.of("error", "Login required"));
        User user = resolveUser(ud);
        if (user == null) return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        if (!isPaid(user)) return paymentRequired();
        try { return ResponseEntity.ok(programService.getActivePrograms()); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @GetMapping("/user/programs/{id}/content")
    public ResponseEntity<?> content(@PathVariable Long id,
                                     @AuthenticationPrincipal UserDetails ud) {
        if (ud == null) return ResponseEntity.status(401).body(Map.of("error", "Login required"));
        User user = resolveUser(ud);
        if (user == null) return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        if (!isPaid(user)) return paymentRequired();
        try { return ResponseEntity.ok(programService.getContent(id)); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @GetMapping("/user/programs/{id}/files")
    public ResponseEntity<?> programFiles(@PathVariable Long id,
                                          @AuthenticationPrincipal UserDetails ud) {
        if (ud == null) return ResponseEntity.status(401).body(Map.of("error", "Login required"));
        User user = resolveUser(ud);
        if (user == null) return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        if (!isPaid(user)) return paymentRequired();
        try { return ResponseEntity.ok(fileUploadService.getFiles(id)); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @GetMapping("/user/sessions")
    public ResponseEntity<?> allSessions(@AuthenticationPrincipal UserDetails ud) {
        if (ud == null) return ResponseEntity.status(401).body(Map.of("error", "Login required"));
        User user = resolveUser(ud);
        if (user == null) return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        if (!isPaid(user)) return paymentRequired();
        try { return ResponseEntity.ok(programService.getAllActiveSessions()); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @GetMapping("/program/{id}/details")
    public ResponseEntity<?> programDetails(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails ud) {
        if (ud == null) return ResponseEntity.status(401).body(Map.of("error", "Login required"));
        User user = resolveUser(ud);
        if (user == null) return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        if (!isPaid(user)) return ResponseEntity.status(402).body(Map.of(
            "error",   "Lifetime membership required",
            "code",    "PAYMENT_REQUIRED",
            "message", "Unlock Lifetime Access to view content."
        ));
        try { return ResponseEntity.ok(programService.getProgramDetails(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/user/community")
    public ResponseEntity<?> community(@AuthenticationPrincipal UserDetails ud) {
        if (ud == null) return ResponseEntity.status(401).body(Map.of("error", "Login required"));
        User user = resolveUser(ud);
        if (user == null) return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        if (!isPaid(user)) return paymentRequired();
        try {
            return ResponseEntity.ok(waGroupRepo.findByActiveTrueOrderByCreatedAtDesc());
        } catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @GetMapping("/user/payments")
    public ResponseEntity<?> myPayments(@AuthenticationPrincipal UserDetails ud) {
        if (ud == null) return ResponseEntity.status(401).body(Map.of("error", "Login required"));
        User user = resolveUser(ud);
        if (user == null) return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        try { return ResponseEntity.ok(paymentService.getUserPayments(user.getId())); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @GetMapping("/user/status")
    public ResponseEntity<?> myStatus(@AuthenticationPrincipal UserDetails ud) {
        if (ud == null) return ResponseEntity.status(401).body(Map.of("error", "Login required"));
        User user = resolveUser(ud);
        if (user == null) return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        return ResponseEntity.ok(Map.of(
            "name",               user.getName(),
            "email",              user.getEmail(),
            "subscriptionStatus", user.getSubscriptionStatus().name(),
            "role",               user.getRole().name()
        ));
    }

    private User resolveUser(UserDetails ud) {
        try { return userRepo.findByEmail(ud.getUsername()).orElse(null); }
        catch (Exception e) { return null; }
    }

    private boolean isPaid(User user) {
        return user.getSubscriptionStatus() == User.SubscriptionStatus.PAID;
    }

    private ResponseEntity<Map<String, Object>> paymentRequired() {
        return ResponseEntity.status(402).body(Map.of(
            "error",   "Lifetime membership required",
            "code",    "PAYMENT_REQUIRED",
            "message", "Purchase lifetime access to unlock all programs, content, and live sessions."
        ));
    }
}
