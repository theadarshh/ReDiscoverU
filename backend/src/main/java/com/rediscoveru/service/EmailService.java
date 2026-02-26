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

    @Value("${otp.expiry.minutes:5}")                    private int    otpExpiryMinutes;
    @Value("${spring.mail.username:noreply@rediscoveru.life}") private String fromEmail;
    @Value("${app.base.url:http://localhost:3000}")       private String baseUrl;

    // ── Registration OTP ──────────────────────────────────────────
    @Transactional
    public void sendRegistrationOtp(User user) {
        evtRepo.deleteByUserId(user.getId());
        String otp = generateOtp();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setOtp(otp);
        token.setExpiryTime(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        evtRepo.save(token);
        sendHtml(user.getEmail(),
                "ReDiscoverU — Verify Your Email",
                buildOtpHtml("Email Verification", user.getName(), otp,
                        "Enter this code to verify your email address."));
    }

    // ── Verify registration OTP ───────────────────────────────────
    @Transactional
    public void verifyRegistrationOtp(String email, String otp) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        if (user.getSubscriptionStatus() == User.SubscriptionStatus.PAID)
            throw new RuntimeException("Account is already active");

        EmailVerificationToken token = evtRepo
                .findTopByUserEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new RuntimeException("No verification code found"));

        if (token.isVerified())
            throw new RuntimeException("Code already used — request a new one");
        if (token.getExpiryTime().isBefore(LocalDateTime.now()))
            throw new RuntimeException("Code has expired — please request a new one");
        if (!token.getOtp().equals(otp))
            throw new RuntimeException("Incorrect code");

        token.setVerified(true);
        evtRepo.save(token);
        user.setEnabled(true);
        userRepo.save(user);
    }

    // ── Resend registration OTP ───────────────────────────────────
    @Transactional
    public void resendRegistrationOtp(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        sendRegistrationOtp(user);
    }

    // ── Password reset — send OTP email ───────────────────────────
    @Transactional
    public void sendPasswordResetOtp(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with that email"));
        prtRepo.deleteByUserId(user.getId());
        String otp = generateOtp();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setOtp(otp);
        token.setExpiryTime(LocalDateTime.now().plusMinutes(10));
        prtRepo.save(token);
        sendHtml(email, "ReDiscoverU — Reset Your Password",
                buildOtpHtml("Password Reset", user.getName(), otp,
                        "Use this code to reset your password. It expires in 10 minutes."));
    }

    // ── Password reset — verify OTP and change password ───────────
    // Called by AuthService.resetPassword()
    @Transactional
    public void verifyPasswordResetOtp(String email, String otp, String newPasswordPlain) {
        PasswordResetToken token = prtRepo.findTopByUserEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new RuntimeException("No reset code found"));
        if (token.isUsed())
            throw new RuntimeException("Code already used");
        if (token.getExpiryTime().isBefore(LocalDateTime.now()))
            throw new RuntimeException("Code expired — request a new one");
        if (!token.getOtp().equals(otp))
            throw new RuntimeException("Incorrect code");
        token.setUsed(true);
        prtRepo.save(token);
        User user = token.getUser();
        user.setPassword(new BCryptPasswordEncoder().encode(newPasswordPlain));
        userRepo.save(user);
    }

    // ── Session notification — async, non-blocking ────────────────
    @Async
    public void notifyPaidUsersOfSession(LiveSession session) {
        try {
            List<User> paidUsers = userRepo.findBySubscriptionStatus(User.SubscriptionStatus.PAID);
            String programName = session.getProgram() != null
                    ? session.getProgram().getTitle() : "ReDiscoverU";
            String scheduleDisplay = buildScheduleDisplay(session);

            for (User user : paidUsers) {
                try {
                    sendHtml(user.getEmail(),
                            "Your Upcoming Session – ReDiscoverU",
                            buildSessionHtml(user, session, programName, scheduleDisplay));
                } catch (Exception e) {
                    System.err.println("Failed to notify " + user.getEmail() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("notifyPaidUsersOfSession failed: " + e.getMessage());
        }
    }

    // ── Cleanup expired tokens — hourly ───────────────────────────
    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void cleanExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            evtRepo.deleteByExpiryTimeBefore(now);
            prtRepo.deleteByExpiryTimeBefore(now);
        } catch (Exception e) {
            System.err.println("Token cleanup failed: " + e.getMessage());
        }
    }

    // ── Private: send HTML email safely ──────────────────────────
    private void sendHtml(String to, String subject, String htmlBody) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail, "ReDiscoverU");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("Email send failed to " + to + ": " + e.getMessage());
        }
    }

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    // ── HTML: OTP email ───────────────────────────────────────────
    private String buildOtpHtml(String type, String name, String otp, String note) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='" +
               "margin:0;padding:0;background:#020617;font-family:Georgia,serif;'>" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='background:#020617;padding:40px 0;'>" +
               "<tr><td align='center'><table width='560' cellpadding='0' cellspacing='0' " +
               "style='background:#0b1120;border:1px solid #1e293b;'>" +
               "<tr><td><div style='height:3px;background:linear-gradient(90deg,#C6A85C,transparent);'></div></td></tr>" +
               "<tr><td style='padding:44px 48px 36px;'>" +
               "<div style='font-size:11px;letter-spacing:3px;text-transform:uppercase;color:#C6A85C;margin-bottom:28px;'>" +
               "ReDiscoverU · " + type + "</div>" +
               "<h2 style='margin:0 0 16px;font-weight:400;font-size:26px;color:#f1f5f9;'>Hello, " + name + "</h2>" +
               "<p style='margin:0 0 32px;color:#94a3b8;font-size:15px;line-height:1.7;'>" + note + "</p>" +
               "<div style='background:#020617;border:1px solid rgba(198,168,92,0.3);padding:28px;text-align:center;margin:0 0 32px;'>" +
               "<div style='font-size:38px;letter-spacing:10px;color:#C6A85C;font-weight:300;'>" + otp + "</div>" +
               "<div style='font-size:11px;color:#475569;margin-top:10px;letter-spacing:2px;text-transform:uppercase;'>Verification Code</div>" +
               "</div>" +
               "<p style='margin:0;color:#475569;font-size:12px;line-height:1.6;'>Do not share this code with anyone.</p>" +
               "</td></tr><tr><td style='padding:24px 48px;border-top:1px solid #1e293b;'>" +
               "<p style='margin:0;font-size:11px;color:#334155;letter-spacing:1px;text-transform:uppercase;'>ReDiscoverU · Bengaluru, India</p>" +
               "</td></tr></table></td></tr></table></body></html>";
    }

    // ── HTML: Session notification ────────────────────────────────
    private String buildSessionHtml(User user, LiveSession session, String programName, String schedule) {
        String joinBtn = session.getMeetingLink() != null
                ? "<a href='" + session.getMeetingLink() + "' style='display:inline-block;background:#C6A85C;" +
                  "color:#020617;padding:14px 32px;text-decoration:none;font-size:13px;letter-spacing:1px;" +
                  "text-transform:uppercase;'>Join Session →</a>"
                : "<p style='color:#64748b;font-size:13px;'>Meeting link will be shared soon.</p>";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='" +
               "margin:0;padding:0;background:#020617;font-family:Georgia,serif;'>" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='background:#020617;padding:40px 0;'>" +
               "<tr><td align='center'><table width='580' cellpadding='0' cellspacing='0' " +
               "style='background:#0b1120;border:1px solid #1e293b;'>" +
               "<tr><td><div style='height:3px;background:linear-gradient(90deg,#C6A85C,transparent);'></div></td></tr>" +
               "<tr><td style='padding:44px 48px 16px;'>" +
               "<div style='font-size:10px;letter-spacing:3px;text-transform:uppercase;color:#C6A85C;margin-bottom:24px;'>Session Announcement</div>" +
               "<h2 style='margin:0 0 8px;font-weight:400;font-size:28px;color:#f1f5f9;'>Hello, " +
               user.getName().split(" ")[0] + "</h2>" +
               "<p style='margin:0 0 32px;color:#94a3b8;font-size:15px;line-height:1.7;'>" +
               "A session has been scheduled. Show up with intention.</p>" +
               "<div style='background:#020617;border:1px solid #1e293b;padding:28px 32px;margin-bottom:28px;'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'>" +
               "<tr><td style='padding:8px 0;border-bottom:1px solid #1e293b;'>" +
               "<span style='color:#475569;font-size:11px;letter-spacing:2px;text-transform:uppercase;'>Session</span><br>" +
               "<span style='color:#f1f5f9;font-size:16px;'>" + session.getTitle() + "</span></td></tr>" +
               "<tr><td style='padding:8px 0;border-bottom:1px solid #1e293b;'>" +
               "<span style='color:#475569;font-size:11px;letter-spacing:2px;text-transform:uppercase;'>Program</span><br>" +
               "<span style='color:#f1f5f9;font-size:16px;'>" + programName + "</span></td></tr>" +
               "<tr><td style='padding:8px 0;'>" +
               "<span style='color:#475569;font-size:11px;letter-spacing:2px;text-transform:uppercase;'>Schedule</span><br>" +
               "<span style='color:#C6A85C;font-size:15px;'>" + schedule + "</span></td></tr>" +
               "</table></div><div style='text-align:center;'>" + joinBtn + "</div></td></tr>" +
               "<tr><td style='padding:24px 48px;border-top:1px solid #1e293b;'>" +
               "<p style='margin:0;font-size:11px;color:#334155;letter-spacing:1px;text-transform:uppercase;'>ReDiscoverU · Bengaluru, India · Jayashankar Lingaiah</p>" +
               "</td></tr></table></td></tr></table></body></html>";
    }

    private String buildScheduleDisplay(LiveSession s) {
        StringBuilder sb = new StringBuilder();
        if (s.getRecurrenceType() != null) {
            switch (s.getRecurrenceType()) {
                case DAILY    -> sb.append("Every Day");
                case WEEKDAYS -> sb.append("Mon – Fri");
                case CUSTOM_DAYS -> sb.append(s.getRecurrenceDays() != null
                        ? s.getRecurrenceDays().replace(",", " · ") : "Custom Days");
                case SPECIFIC_DATE_RANGE -> {
                    if (s.getValidFrom() != null) sb.append(s.getValidFrom());
                    if (s.getValidTo()   != null) sb.append(" to ").append(s.getValidTo());
                }
                default -> sb.append(s.getSpecificDate() != null ? s.getSpecificDate() : "One-time");
            }
        }
        if (s.getStartTime() != null) {
            sb.append(" · ").append(s.getStartTime());
            if (s.getEndTime() != null) sb.append(" – ").append(s.getEndTime());
            sb.append(" ").append(s.getTimezone() != null
                    ? s.getTimezone().replace("Asia/Kolkata", "IST") : "IST");
        }
        return sb.toString();
    }
}
