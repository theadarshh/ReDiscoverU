package com.rediscoveru.service;

import com.rediscoveru.entity.*;
import com.rediscoveru.repository.*;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service @RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender                   mailSender;
    private final EmailVerificationTokenRepository evtRepo;
    private final PasswordResetTokenRepository     prtRepo;
    private final UserRepository                   userRepo;
    private final AnnouncementRepository           announcementRepo;

    @Value("${otp.expiry.minutes:5}")     private int    otpExpiry;
    @Value("${spring.mail.username}")     private String fromEmail;
    @Value("${admin.email:admin@rediscoveru.life}") private String adminEmail;

    // ── Registration OTP ────────────────────────────────────────
    @Transactional
    public void sendRegistrationOtp(User user) {
        evtRepo.deleteByUserId(user.getId());
        String otp = generateOtp();
        EmailVerificationToken t = new EmailVerificationToken();
        t.setUser(user);
        t.setOtp(otp);
        t.setExpiryTime(LocalDateTime.now().plusMinutes(otpExpiry));
        evtRepo.save(t);
        sendHtml(user.getEmail(),
                "ReDiscoverU — Verify Your Email",
                otpHtml("Email Verification", user.getName(), otp,
                        "Enter this code to verify your email address."));
    }

    @Transactional
    public void verifyRegistrationOtp(String email, String otp) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        if (user.getSubscriptionStatus() == User.SubscriptionStatus.PAID)
            throw new RuntimeException("Account is already active");
        EmailVerificationToken t = evtRepo
                .findTopByUserEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new RuntimeException("No verification code found"));
        if (t.isVerified())
            throw new RuntimeException("Code already used — request a new one");
        if (t.getExpiryTime().isBefore(LocalDateTime.now()))
            throw new RuntimeException("Code expired — request a new one");
        if (!t.getOtp().equals(otp))
            throw new RuntimeException("Incorrect code");
        t.setVerified(true);
        evtRepo.save(t);
        user.setEnabled(true);
        userRepo.save(user);
    }

    @Transactional
    public void resendRegistrationOtp(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        sendRegistrationOtp(user);
    }

    // ── Password reset ───────────────────────────────────────────
    @Transactional
    public void sendPasswordResetOtp(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found"));
        prtRepo.deleteByUserId(user.getId());
        String otp = generateOtp();
        PasswordResetToken t = new PasswordResetToken();
        t.setUser(user);
        t.setOtp(otp);
        t.setExpiryTime(LocalDateTime.now().plusMinutes(10));
        prtRepo.save(t);
        sendHtml(email, "ReDiscoverU — Reset Your Password",
                otpHtml("Password Reset", user.getName(), otp,
                        "Use this code to reset your password. Expires in 10 minutes."));
    }

    @Transactional
    public void verifyPasswordResetOtp(String email, String otp, String newPassword) {
        PasswordResetToken t = prtRepo.findTopByUserEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new RuntimeException("No reset code found"));
        if (t.isUsed())
            throw new RuntimeException("Code already used");
        if (t.getExpiryTime().isBefore(LocalDateTime.now()))
            throw new RuntimeException("Code expired — request a new one");
        if (!t.getOtp().equals(otp))
            throw new RuntimeException("Incorrect code");
        t.setUsed(true);
        prtRepo.save(t);
        User user = t.getUser();
        user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
        userRepo.save(user);
    }

    // ── Admin notification: new registration ────────────────────
    @Async
    public void notifyAdminNewRegistration(User user) {
        try {
            String html = base("Admin Notification") +
                "<tr><td style='padding:36px 48px;'>" +
                "<div style='font-size:11px;letter-spacing:3px;text-transform:uppercase;color:#C6A85C;margin-bottom:20px;'>New Registration</div>" +
                "<p style='color:#f1f5f9;font-size:16px;margin:0 0 20px;'>A new user has registered on ReDiscoverU.</p>" +
                "<table style='width:100%;border-collapse:collapse;'>" +
                row("Name",  user.getName()) +
                row("Email", user.getEmail()) +
                row("Phone", user.getPhoneNumber() != null ? user.getPhoneNumber() : "—") +
                row("Time",  LocalDateTime.now().toString()) +
                "</table></td></tr>" + footer();
            sendHtml(adminEmail, "New Registration — ReDiscoverU", html);
        } catch (Exception e) {
            System.err.println("[Email] Admin notify failed: " + e.getMessage());
        }
    }

    // ── Welcome email after payment ──────────────────────────────
    @Async
    public void sendWelcomeEmail(User user) {
        try {
            String firstName = user.getName().split(" ")[0];
            String html = base("Welcome") +
                "<tr><td style='padding:44px 48px 36px;'>" +
                "<div style='font-size:11px;letter-spacing:3px;text-transform:uppercase;color:#C6A85C;margin-bottom:24px;'>Membership Activated</div>" +
                "<h2 style='margin:0 0 12px;font-weight:400;font-size:26px;color:#f1f5f9;'>Congratulations, " + esc(firstName) + "! 🎉</h2>" +
                "<p style='margin:0 0 24px;color:#94a3b8;font-size:15px;line-height:1.7;'>Your lifetime membership is now <strong style='color:#C6A85C;'>active</strong>.</p>" +
                "<div style='background:#020617;border:1px solid rgba(198,168,92,0.3);padding:24px;margin-bottom:24px;'>" +
                "<p style='color:#C6A85C;font-size:12px;letter-spacing:2px;text-transform:uppercase;margin:0 0 12px;'>You now have lifetime access to</p>" +
                "<ul style='margin:0;padding-left:16px;color:#94a3b8;font-size:14px;line-height:2;'>" +
                "<li>All self-paced programs &amp; recordings</li><li>Live Google Meet sessions (Tue &amp; Thu)</li>" +
                "<li>1-to-1 Mentorship materials</li><li>All future programs added to the platform</li>" +
                "</ul></div>" +
                "<p style='color:#64748b;font-size:13px;margin:0;'>Log in to your dashboard to begin your journey.</p>" +
                "</td></tr>" + footer();
            sendHtml(user.getEmail(), "Welcome to ReDiscoverU 🎉 — Lifetime Access Activated", html);
        } catch (Exception e) {
            System.err.println("[Email] Welcome email failed: " + e.getMessage());
        }
    }

    // ── Session notification ─────────────────────────────────────
    @Async
    public void notifyPaidUsersOfSession(LiveSession session) {
        try {
            List<User> paid = userRepo.findBySubscriptionStatus(User.SubscriptionStatus.PAID);
            String programName = session.getProgram() != null ? session.getProgram().getTitle() : "ReDiscoverU";
            for (User u : paid) {
                try { sendHtml(u.getEmail(), "New Session — ReDiscoverU", sessionHtml(u, session, programName)); }
                catch (Exception e) { System.err.println("[Email] Session to " + u.getEmail() + ": " + e.getMessage()); }
            }
        } catch (Exception e) { System.err.println("[Email] notifySession: " + e.getMessage()); }
    }

    // ── Broadcast email ──────────────────────────────────────────
    @Async
    public void broadcastToAllPaidUsers(String subject, String message) {
        try {
            List<User> paid = userRepo.findBySubscriptionStatus(User.SubscriptionStatus.PAID);
            for (User u : paid) {
                try {
                    String html = base("Message from ReDiscoverU") +
                        "<tr><td style='padding:36px 48px;'>" +
                        "<div style='font-size:11px;letter-spacing:2px;text-transform:uppercase;color:#C6A85C;margin-bottom:16px;'>Message for You</div>" +
                        "<h2 style='margin:0 0 8px;font-weight:400;font-size:22px;color:#f1f5f9;'>Hello, " + esc(u.getName().split(" ")[0]) + "</h2>" +
                        "<div style='background:#020617;border-left:3px solid #C6A85C;padding:18px 20px;margin:20px 0;'>" +
                        "<div style='font-size:15px;color:#f1f5f9;margin-bottom:10px;font-weight:500;'>" + esc(subject) + "</div>" +
                        "<div style='font-size:14px;color:#94a3b8;line-height:1.9;'>" + esc(message).replace("\n", "<br>") + "</div>" +
                        "</div></td></tr>" + footer();
                    sendHtml(u.getEmail(), subject, html);
                } catch (Exception e) { System.err.println("[Email] Broadcast to " + u.getEmail() + ": " + e.getMessage()); }
            }
        } catch (Exception e) { System.err.println("[Email] broadcast: " + e.getMessage()); }
    }

    // ── Announcement email ────────────────────────────────────────
    @Async
    public void sendAnnouncementEmail(String title, String message) {
        try {
            List<User> paid = userRepo.findBySubscriptionStatus(User.SubscriptionStatus.PAID);
            for (User u : paid) {
                try {
                    String html = base("Announcement") +
                        "<tr><td style='padding:36px 48px;'>" +
                        "<div style='font-size:11px;letter-spacing:2px;text-transform:uppercase;color:#C6A85C;margin-bottom:16px;'>Announcement</div>" +
                        "<h2 style='margin:0 0 8px;font-weight:400;font-size:22px;color:#f1f5f9;'>Hello, " + esc(u.getName().split(" ")[0]) + "</h2>" +
                        "<div style='background:#020617;border:1px solid rgba(198,168,92,0.3);padding:20px 24px;margin:20px 0;'>" +
                        "<div style='font-size:16px;color:#f1f5f9;margin-bottom:10px;'>" + esc(title) + "</div>" +
                        "<div style='font-size:14px;color:#94a3b8;line-height:1.85;'>" + esc(message).replace("\n", "<br>") + "</div>" +
                        "</div></td></tr>" + footer();
                    sendHtml(u.getEmail(), title + " — ReDiscoverU", html);
                } catch (Exception e) { System.err.println("[Email] Ann to " + u.getEmail() + ": " + e.getMessage()); }
            }
        } catch (Exception e) { System.err.println("[Email] sendAnn: " + e.getMessage()); }
    }

    // ── Scheduled cleanup ────────────────────────────────────────
    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void cleanExpired() {
        try {
            LocalDateTime now = LocalDateTime.now();
            evtRepo.deleteByExpiryTimeBefore(now);
            prtRepo.deleteByExpiryTimeBefore(now);
            announcementRepo.deleteByExpiresAtBefore(now);
        } catch (Exception e) { System.err.println("[Email] cleanup: " + e.getMessage()); }
    }

    // ── Core send ────────────────────────────────────────────────
    private void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(fromEmail, "ReDiscoverU");
            h.setTo(to);
            h.setSubject(subject);
            h.setText(html, true);
            mailSender.send(msg);
        } catch (Exception e) { System.err.println("[Email] send to " + to + ": " + e.getMessage()); }
    }

    // ── HTML helpers ─────────────────────────────────────────────
    private String base(String type) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>" +
               "<body style='margin:0;padding:0;background:#020617;font-family:Georgia,serif;'>" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='background:#020617;padding:40px 0;'>" +
               "<tr><td align='center'><table width='580' cellpadding='0' cellspacing='0' " +
               "style='background:#0b1120;border:1px solid #1e293b;'>" +
               "<tr><td><div style='height:3px;background:linear-gradient(90deg,#C6A85C,transparent);'></div></td></tr>";
    }

    private String footer() {
        return "<tr><td style='padding:20px 48px;border-top:1px solid #1e293b;'>" +
               "<p style='margin:0;font-size:11px;color:#334155;letter-spacing:1px;text-transform:uppercase;'>" +
               "ReDiscoverU · Bengaluru, India · Jayashankar Lingaiah</p>" +
               "</td></tr></table></td></tr></table></body></html>";
    }

    private String otpHtml(String type, String name, String otp, String note) {
        return base(type) +
               "<tr><td style='padding:44px 48px 36px;'>" +
               "<div style='font-size:11px;letter-spacing:3px;text-transform:uppercase;color:#C6A85C;margin-bottom:28px;'>" +
               "ReDiscoverU · " + type + "</div>" +
               "<h2 style='margin:0 0 16px;font-weight:400;font-size:26px;color:#f1f5f9;'>Hello, " + esc(name) + "</h2>" +
               "<p style='margin:0 0 32px;color:#94a3b8;font-size:15px;line-height:1.7;'>" + note + "</p>" +
               "<div style='background:#020617;border:1px solid rgba(198,168,92,0.3);padding:28px;text-align:center;margin:0 0 32px;'>" +
               "<div style='font-size:38px;letter-spacing:10px;color:#C6A85C;font-weight:300;'>" + otp + "</div>" +
               "<div style='font-size:11px;color:#475569;margin-top:10px;letter-spacing:2px;text-transform:uppercase;'>Verification Code</div>" +
               "</div><p style='margin:0;color:#475569;font-size:12px;'>Do not share this code.</p>" +
               "</td></tr>" + footer();
    }

    private String sessionHtml(User user, LiveSession session, String programName) {
        String firstName = user.getName().split(" ")[0];
        String joinBtn = session.getMeetingLink() != null
            ? "<a href='" + session.getMeetingLink() + "' style='display:inline-block;background:#C6A85C;color:#020617;padding:14px 32px;text-decoration:none;font-size:13px;letter-spacing:1px;text-transform:uppercase;'>Join →</a>"
            : "<p style='color:#64748b;font-size:13px;'>Meeting link coming soon.</p>";
        return base("Session") +
               "<tr><td style='padding:36px 48px;'>" +
               "<div style='font-size:11px;letter-spacing:2px;text-transform:uppercase;color:#C6A85C;margin-bottom:16px;'>Session Scheduled</div>" +
               "<h2 style='margin:0 0 8px;font-weight:400;font-size:24px;color:#f1f5f9;'>Hello, " + esc(firstName) + "</h2>" +
               "<div style='background:#020617;border:1px solid #1e293b;padding:20px 24px;margin:20px 0;'>" +
               row("Session", session.getTitle()) + row("Program", programName) +
               "</div><div style='text-align:center;'>" + joinBtn + "</div>" +
               "</td></tr>" + footer();
    }

    private String row(String label, String value) {
        return "<tr><td style='padding:8px 0;border-bottom:1px solid #1e293b;color:#475569;font-size:12px;text-transform:uppercase;letter-spacing:1px;width:100px;'>" +
               label + "</td><td style='padding:8px 0;border-bottom:1px solid #1e293b;color:#f1f5f9;font-size:14px;'>" + esc(value) + "</td></tr>";
    }

    private String generateOtp() { return String.format("%06d", new Random().nextInt(999999)); }
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}
