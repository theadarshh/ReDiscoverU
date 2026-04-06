package com.rediscoveru.controller;

import com.rediscoveru.entity.*;
import com.rediscoveru.repository.*;
import com.rediscoveru.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ProgramService             programService;
    private final AdminService               adminService;
    private final PaymentService             paymentService;
    private final PaymentConfigService       paymentConfigService;
    private final EmailService               emailService;
    private final CategoryService            categoryService;
    private final FileUploadService          fileUploadService;
    private final PlatformSettingsRepository settingsRepo;
    private final WhatsAppGroupRepository    waGroupRepo;
    private final MotivationPostRepository   motivationRepo;
    private final AnnouncementRepository     announcementRepo;
    private final MentorService              mentorService;

    @Value("${file.upload.dir:uploads}")           private String uploadDir;
    @Value("${admin.email:admin@rediscoveru.life}") private String primaryAdminEmail;

    // ── Stats ─────────────────────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        try { return ResponseEntity.ok(adminService.getStats()); }
        catch (Exception e) { return ResponseEntity.ok(Map.of("totalUsers",0,"paidUsers",0,"pendingUsers",0,"totalPrograms",0,"totalRevenue",0)); }
    }

    // ── Platform Settings ──────────────────────────────────────────
    @GetMapping("/settings")
    public ResponseEntity<?> getSettings() {
        try { return ResponseEntity.ok(settingsRepo.findById(1L).orElse(new PlatformSettings())); }
        catch (Exception e) { return ResponseEntity.ok(Map.of("id",1,"lifetimePrice",499.00)); }
    }

    @PatchMapping("/settings/price")
    public ResponseEntity<?> updatePrice(@RequestBody Map<String, Object> body) {
        try {
            BigDecimal price = new BigDecimal(String.valueOf(body.get("lifetimePrice")));
            PlatformSettings s = settingsRepo.findById(1L).orElse(new PlatformSettings());
            s.setId(1L); s.setLifetimePrice(price);
            settingsRepo.save(s);
            return ResponseEntity.ok(Map.of("lifetimePrice", price, "message", "Price updated"));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── Payment Config ─────────────────────────────────────────────
    @GetMapping("/payment-config")
    public ResponseEntity<?> getPaymentConfig() {
        try { return ResponseEntity.ok(paymentConfigService.getAdminView()); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/payment-config")
    public ResponseEntity<?> savePaymentConfig(@RequestBody Map<String, Object> body) {
        try {
            String keyId         = body.containsKey("keyId")         ? String.valueOf(body.get("keyId")).trim()         : null;
            String keySecret     = body.containsKey("keySecret")     ? String.valueOf(body.get("keySecret")).trim()     : null;
            String webhookSecret = body.containsKey("webhookSecret") ? String.valueOf(body.get("webhookSecret")).trim() : null;
            boolean enabled      = Boolean.TRUE.equals(body.get("enabled"));
            return ResponseEntity.ok(paymentConfigService.save(
                    (keyId != null && !keyId.isBlank()) ? keyId : null,
                    (keySecret != null && !keySecret.isBlank()) ? keySecret : null,
                    (webhookSecret != null && !webhookSecret.isBlank()) ? webhookSecret : null,
                    enabled));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── Categories ─────────────────────────────────────────────────
    @GetMapping("/categories")
    public ResponseEntity<?> categories() {
        try { return ResponseEntity.ok(categoryService.getAll()); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(@RequestBody ProgramCategory c) {
        try { return ResponseEntity.ok(categoryService.create(c)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody ProgramCategory c) {
        try { return ResponseEntity.ok(categoryService.update(id, c)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PatchMapping("/categories/{id}/toggle")
    public ResponseEntity<?> toggleCategory(@PathVariable Long id) {
        try { categoryService.toggle(id); return ResponseEntity.ok(Map.of("ok", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try { categoryService.delete(id); return ResponseEntity.ok(Map.of("deleted", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── Programs ────────────────────────────────────────────────────
    @GetMapping("/programs")
    public ResponseEntity<?> programs() {
        try { return ResponseEntity.ok(programService.getAllPrograms()); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @PostMapping("/programs")
    public ResponseEntity<?> createProgram(@RequestBody Program p) {
        try { return ResponseEntity.ok(programService.create(p)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/programs/{id}")
    public ResponseEntity<?> updateProgram(@PathVariable Long id, @RequestBody Program p) {
        try { return ResponseEntity.ok(programService.update(id, p)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/programs/{id}")
    public ResponseEntity<?> deleteProgram(@PathVariable Long id) {
        try { programService.delete(id); return ResponseEntity.ok(Map.of("deleted", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PatchMapping("/programs/{id}/toggle")
    public ResponseEntity<?> toggleProgram(@PathVariable Long id) {
        try { programService.toggleActive(id); return ResponseEntity.ok(Map.of("ok", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── Content ─────────────────────────────────────────────────────
    @GetMapping("/programs/{id}/content")
    public ResponseEntity<?> getContent(@PathVariable Long id) {
        try { return ResponseEntity.ok(programService.getContent(id)); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @PostMapping("/programs/{id}/content")
    public ResponseEntity<?> addContent(@PathVariable Long id, @RequestBody ProgramContent c) {
        try { return ResponseEntity.ok(programService.addContent(id, c)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/content/{cid}")
    public ResponseEntity<?> updateContent(@PathVariable Long cid, @RequestBody ProgramContent c) {
        try { return ResponseEntity.ok(programService.updateContent(cid, c)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/content/{cid}")
    public ResponseEntity<?> deleteContent(@PathVariable Long cid) {
        try { programService.deleteContent(cid); return ResponseEntity.ok(Map.of("deleted", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── File Upload ──────────────────────────────────────────────────
    @GetMapping("/programs/{id}/files")
    public ResponseEntity<?> getFiles(@PathVariable Long id) {
        try { return ResponseEntity.ok(fileUploadService.getFiles(id)); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @PostMapping("/programs/{id}/files/upload")
    public ResponseEntity<?> uploadFile(@PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value="title", defaultValue="") String title,
            @RequestParam(value="orderIndex", defaultValue="0") int orderIndex) {
        try { return ResponseEntity.ok(fileUploadService.upload(id, file, title, orderIndex)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "Upload failed: " + e.getMessage())); }
    }

    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable Long fileId) {
        try { fileUploadService.delete(fileId); return ResponseEntity.ok(Map.of("deleted", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── Live Sessions ─────────────────────────────────────────────
    @GetMapping("/programs/{id}/sessions")
    public ResponseEntity<?> getSessions(@PathVariable Long id) {
        try { return ResponseEntity.ok(programService.getSessions(id)); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @PostMapping("/programs/{id}/sessions")
    public ResponseEntity<?> addSession(@PathVariable Long id, @RequestBody LiveSession s) {
        try {
            LiveSession saved = programService.addSession(id, s);
            try { emailService.notifyPaidUsersOfSession(saved); } catch (Exception ignored) {}
            return ResponseEntity.ok(saved);
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/sessions/{sid}")
    public ResponseEntity<?> updateSession(@PathVariable Long sid, @RequestBody LiveSession s) {
        try { return ResponseEntity.ok(programService.updateSession(sid, s)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/sessions/{sid}")
    public ResponseEntity<?> deleteSession(@PathVariable Long sid) {
        try { programService.deleteSession(sid); return ResponseEntity.ok(Map.of("deleted", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PatchMapping("/sessions/{sid}/toggle")
    public ResponseEntity<?> toggleSession(@PathVariable Long sid) {
        try { programService.toggleSession(sid); return ResponseEntity.ok(Map.of("ok", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── Coupons ────────────────────────────────────────────────────
    @GetMapping("/coupons")
    public ResponseEntity<?> coupons() {
        try { return ResponseEntity.ok(adminService.getCoupons()); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @PostMapping("/coupons")
    public ResponseEntity<?> createCoupon(@RequestBody Coupon c) {
        try { return ResponseEntity.ok(adminService.createCoupon(c)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PatchMapping("/coupons/{id}/toggle")
    public ResponseEntity<?> toggleCoupon(@PathVariable Long id) {
        try { adminService.toggleCoupon(id); return ResponseEntity.ok(Map.of("ok", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── Users ──────────────────────────────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<?> users() {
        try { return ResponseEntity.ok(adminService.getAllUsers()); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @PatchMapping("/users/{id}/activate")
    public ResponseEntity<?> activateUser(@PathVariable Long id) {
        try { adminService.activateUser(id); return ResponseEntity.ok(Map.of("ok", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PatchMapping("/users/{id}/toggle")
    public ResponseEntity<?> toggleUser(@PathVariable Long id) {
        try { adminService.toggleUserEnabled(id); return ResponseEntity.ok(Map.of("ok", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> changeRole(@PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails ud) {
        try {
            String roleStr = body.get("role");
            if (roleStr == null) return ResponseEntity.badRequest().body(Map.of("error", "role is required"));
            User.Role newRole = User.Role.valueOf(roleStr);
            String requester = ud != null ? ud.getUsername() : "";
            adminService.changeRole(id, newRole, requester, primaryAdminEmail);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── Payments ────────────────────────────────────────────────────
    @GetMapping("/payments")
    public ResponseEntity<?> payments() {
        try { return ResponseEntity.ok(paymentService.getAllPayments()); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    // ── WhatsApp Groups ──────────────────────────────────────────
    @GetMapping("/whatsapp-groups")
    public ResponseEntity<?> waGroups() {
        try { return ResponseEntity.ok(waGroupRepo.findAllByOrderByCreatedAtDesc()); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @PostMapping("/whatsapp-groups")
    public ResponseEntity<?> createWAGroup(@RequestBody WhatsAppGroup g) {
        try {
            if (g.getName()==null||g.getName().isBlank()||g.getUrl()==null||g.getUrl().isBlank())
                return ResponseEntity.badRequest().body(Map.of("error","Name and URL required"));
            g.setActive(true);
            return ResponseEntity.ok(waGroupRepo.save(g));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/whatsapp-groups/{id}")
    public ResponseEntity<?> updateWAGroup(@PathVariable Long id, @RequestBody WhatsAppGroup updated) {
        try {
            WhatsAppGroup g = waGroupRepo.findById(id).orElseThrow();
            if (updated.getName()!=null) g.setName(updated.getName());
            if (updated.getUrl()!=null) g.setUrl(updated.getUrl());
            if (updated.getType()!=null) g.setType(updated.getType());
            if (updated.getDescription()!=null) g.setDescription(updated.getDescription());
            g.setActive(updated.isActive());
            return ResponseEntity.ok(waGroupRepo.save(g));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/whatsapp-groups/{id}")
    public ResponseEntity<?> deleteWAGroup(@PathVariable Long id) {
        try { waGroupRepo.deleteById(id); return ResponseEntity.ok(Map.of("deleted", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PatchMapping("/whatsapp-groups/{id}/toggle")
    public ResponseEntity<?> toggleWAGroup(@PathVariable Long id) {
        try {
            WhatsAppGroup g = waGroupRepo.findById(id).orElseThrow();
            g.setActive(!g.isActive());
            waGroupRepo.save(g);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── Motivation Posts ─────────────────────────────────────────
    @GetMapping("/motivation-posts")
    public ResponseEntity<?> motivationPosts() {
        try { return ResponseEntity.ok(motivationRepo.findAllByOrderByCreatedAtDesc()); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @PostMapping("/motivation-posts")
    public ResponseEntity<?> createPost(@RequestBody MotivationPost p) {
        try {
            if (p.getImagePath()==null||p.getImagePath().isBlank())
                return ResponseEntity.badRequest().body(Map.of("error","Image URL required"));
            p.setActive(true);
            return ResponseEntity.ok(motivationRepo.save(p));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/motivation-posts/upload")
    public ResponseEntity<?> uploadMotivationPost(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value="caption",defaultValue="") String caption) {
        try {
            java.nio.file.Path dir = java.nio.file.Paths.get(uploadDir, "motivation");
            java.nio.file.Files.createDirectories(dir);
            String ext = "jpg";
            String orig = image.getOriginalFilename();
            if (orig != null && orig.contains("."))
                ext = orig.substring(orig.lastIndexOf('.') + 1).toLowerCase();
            // Timestamp-based filename keeps them unique but readable
            String filename = "insp-" + System.currentTimeMillis() + "." + ext;
            java.nio.file.Files.copy(image.getInputStream(), dir.resolve(filename),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            String imagePath = "/uploads/motivation/" + filename;
            MotivationPost post = new MotivationPost();
            post.setImagePath(imagePath);
            post.setCaption(caption);
            post.setActive(true);
            return ResponseEntity.ok(motivationRepo.save(post));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/motivation-posts/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id) {
        try { motivationRepo.deleteById(id); return ResponseEntity.ok(Map.of("deleted", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PatchMapping("/motivation-posts/{id}/toggle")
    public ResponseEntity<?> togglePost(@PathVariable Long id) {
        try {
            MotivationPost p = motivationRepo.findById(id).orElseThrow();
            p.setActive(!p.isActive());
            motivationRepo.save(p);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── Announcements ────────────────────────────────────────────
    @GetMapping("/announcements")
    public ResponseEntity<?> getAnnouncements() {
        try { return ResponseEntity.ok(announcementRepo.findAllByOrderByDisplayOrderAscCreatedAtDesc()); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @PostMapping("/announcements")
    public ResponseEntity<?> createAnnouncement(@RequestBody Map<String, String> body) {
        try {
            String title = body.get("title");
            String message = body.get("message");
            if (title==null||title.isBlank()) return ResponseEntity.badRequest().body(Map.of("error","Title required"));
            if (message==null||message.isBlank()) return ResponseEntity.badRequest().body(Map.of("error","Message required"));
            int nextOrder = (int) announcementRepo.count();
            Announcement a = new Announcement();
            a.setTitle(title.trim()); a.setMessage(message.trim());
            a.setActive(true); a.setDisplayOrder(nextOrder);
            a.setCreatedAt(java.time.LocalDateTime.now());
            a.setExpiresAt(java.time.LocalDateTime.now().plusHours(24));
            Announcement saved = announcementRepo.save(a);
            try { emailService.sendAnnouncementEmail(saved.getTitle(), saved.getMessage()); } catch (Exception ignored) {}
            return ResponseEntity.ok(saved);
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/announcements/{id}")
    public ResponseEntity<?> updateAnnouncement(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            Announcement a = announcementRepo.findById(id).orElseThrow();
            if (body.containsKey("title") && !body.get("title").isBlank()) a.setTitle(body.get("title").trim());
            if (body.containsKey("message") && !body.get("message").isBlank()) a.setMessage(body.get("message").trim());
            a.setExpiresAt(java.time.LocalDateTime.now().plusHours(24));
            return ResponseEntity.ok(announcementRepo.save(a));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/announcements/{id}")
    public ResponseEntity<?> deleteAnnouncement(@PathVariable Long id) {
        try { announcementRepo.deleteById(id); return ResponseEntity.ok(Map.of("deleted", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PatchMapping("/announcements/{id}/toggle")
    public ResponseEntity<?> toggleAnnouncement(@PathVariable Long id) {
        try {
            Announcement a = announcementRepo.findById(id).orElseThrow();
            a.setActive(!a.isActive()); announcementRepo.save(a);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── Broadcast Email ──────────────────────────────────────────
    @PostMapping("/broadcast-email")
    public ResponseEntity<?> broadcastEmail(@RequestBody Map<String, String> body) {
        try {
            String subject = body.get("subject");
            String message = body.get("message");
            if (subject==null||subject.isBlank()) return ResponseEntity.badRequest().body(Map.of("error","Subject required"));
            if (message==null||message.isBlank()) return ResponseEntity.badRequest().body(Map.of("error","Message required"));
            emailService.broadcastToAllPaidUsers(subject, message);
            return ResponseEntity.ok(Map.of("message","Broadcast queued. Emails being sent to all paid members."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── Mentors ──────────────────────────────────────────────────
    @GetMapping("/mentors")
    public ResponseEntity<?> allMentors() {
        try { return ResponseEntity.ok(mentorService.getAll()); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    @PostMapping("/mentors")
    public ResponseEntity<?> createMentor(@RequestBody Mentor m) {
        try { return ResponseEntity.ok(mentorService.create(m)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/mentors/{id}")
    public ResponseEntity<?> updateMentor(@PathVariable Long id, @RequestBody Mentor m) {
        try { return ResponseEntity.ok(mentorService.update(id, m)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PatchMapping("/mentors/{id}/toggle")
    public ResponseEntity<?> toggleMentor(@PathVariable Long id) {
        try { return ResponseEntity.ok(mentorService.toggle(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/mentors/{id}")
    public ResponseEntity<?> deleteMentor(@PathVariable Long id) {
        try { mentorService.delete(id); return ResponseEntity.ok(Map.of("deleted", true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/mentors/{id}/image")
    public ResponseEntity<?> uploadMentorImage(@PathVariable Long id, @RequestParam("image") MultipartFile file) {
        try { return ResponseEntity.ok(mentorService.uploadImage(id, file)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // Quotes are managed via HomepageController at /api/admin/quotes

}
