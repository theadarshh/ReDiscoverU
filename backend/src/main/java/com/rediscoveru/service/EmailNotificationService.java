package com.rediscoveru.service;

import com.rediscoveru.entity.*;
import com.rediscoveru.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * EmailNotificationService
 * ─────────────────────────
 * Sends platform-event emails:
 *  1. notifyAdminNewUser()       — admin email when a user completes payment
 *  2. notifyPaidUsersNewProgram() — email all PAID users when a new program is published
 *  3. notifyPaidUsersNewSession() — email all PAID users when a new session is scheduled
 *  4. sendAnnouncement()          — email all PAID users with a custom announcement
 *
 * All sends are @Async — callers never block waiting for SMTP.
 */
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender  mailSender;
    private final UserRepository  userRepo;

    private final PlatformSettingsService    platformSettingsService;

    @Value("${admin.email:rediscoveruadmin@gmail.com}")
    private String adminEmail;

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ─────────────────────────────────────────────────────────────────────────
    // 1.  Admin notification — new paid user
    // ─────────────────────────────────────────────────────────────────────────
    @Async
    public void notifyAdminNewUser(User user, String paymentStatus) {
        try {
            String joined = user.getCreatedAt() != null
                    ? user.getCreatedAt().format(DISPLAY) : "—";

            String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='" +
                "margin:0;padding:0;background:#f8fafc;font-family:Georgia,serif;'>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f8fafc;padding:40px 0;'>" +
                "<tr><td align='center'><table width='580' cellpadding='0' cellspacing='0' " +
                "style='background:#ffffff;border:1px solid #e2e8f0;'>" +
                // gold bar
                "<tr><td><div style='height:3px;background:linear-gradient(90deg,#C6A85C,transparent);'></div></td></tr>" +
                // header
                "<tr><td style='padding:36px 48px 24px;background:#020617;'>" +
                "<div style='font-size:10px;letter-spacing:3px;text-transform:uppercase;color:#C6A85C;margin-bottom:12px;'>Admin Notification</div>" +
                "<h2 style='margin:0;font-weight:400;font-size:22px;color:#f1f5f9;'>New Member Joined</h2>" +
                "</td></tr>" +
                // body
                "<tr><td style='padding:36px 48px;'>" +
                "<p style='margin:0 0 24px;color:#475569;font-size:15px;line-height:1.7;'>" +
                "A new member has completed enrolment on the ReDiscoverU platform.</p>" +
                // detail table
                "<table width='100%' cellpadding='0' cellspacing='0' style='border:1px solid #e2e8f0;margin-bottom:28px;'>" +
                row("Name",            user.getName()) +
                row("Email",           user.getEmail()) +
                row("Phone",           user.getPhoneNumber() != null ? user.getPhoneNumber() : "—") +
                row("Registered",      joined) +
                row("Payment Status",  paymentStatus) +
                "</table>" +
                "<p style='margin:0;color:#94a3b8;font-size:12px;'>This is an automated notification from ReDiscoverU.</p>" +
                "</td></tr>" +
                // footer
                "<tr><td style='padding:20px 48px;border-top:1px solid #e2e8f0;background:#f8fafc;'>" +
                "<p style='margin:0;font-size:11px;color:#94a3b8;letter-spacing:1px;text-transform:uppercase;'>ReDiscoverU · Bengaluru, India</p>" +
                "</td></tr></table></td></tr></table></body></html>";

            sendHtml(adminEmail, "New Member Joined — ReDiscoverU", html);
        } catch (Exception e) {
            System.err.println("[EmailNotificationService] notifyAdminNewUser failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2.  Notify all paid users — new program published
    // ─────────────────────────────────────────────────────────────────────────
    @Async
    public void notifyPaidUsersNewProgram(Program program) {
        try {
            List<User> paid = userRepo.findBySubscriptionStatus(User.SubscriptionStatus.PAID);
            for (User user : paid) {
                try {
                    String html = programHtml(user, program);
                    sendHtml(user.getEmail(), "New Program Available — ReDiscoverU", html);
                } catch (Exception e) {
                    System.err.println("[EmailNotificationService] Program notify failed for " + user.getEmail() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[EmailNotificationService] notifyPaidUsersNewProgram failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3.  Notify all paid users — new session scheduled
    // ─────────────────────────────────────────────────────────────────────────
    @Async
    public void notifyPaidUsersNewSession(LiveSession session) {
        try {
            List<User> paid = userRepo.findBySubscriptionStatus(User.SubscriptionStatus.PAID);
            String programName = session.getProgram() != null ? session.getProgram().getTitle() : "ReDiscoverU";
            for (User user : paid) {
                try {
                    String html = sessionHtml(user, session, programName);
                    sendHtml(user.getEmail(), "New Session Scheduled — ReDiscoverU", html);
                } catch (Exception e) {
                    System.err.println("[EmailNotificationService] Session notify failed for " + user.getEmail() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[EmailNotificationService] notifyPaidUsersNewSession failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4.  Send announcement to all paid users
    // ─────────────────────────────────────────────────────────────────────────
    @Async
    public void sendAnnouncement(String title, String message) {
        try {
            List<User> paid = userRepo.findBySubscriptionStatus(User.SubscriptionStatus.PAID);
            for (User user : paid) {
                try {
                    String html = announcementHtml(user, title, message);
                    sendHtml(user.getEmail(), title + " — ReDiscoverU", html);
                } catch (Exception e) {
                    System.err.println("[EmailNotificationService] Announcement failed for " + user.getEmail() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[EmailNotificationService] sendAnnouncement failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5.  Welcome email to user after successful payment
    // ─────────────────────────────────────────────────────────────────────────
    @Async
    public void sendWelcomeEmail(User user) {
        try {
            String html = welcomeHtml(user);
            sendHtml(user.getEmail(), "Welcome to ReDiscoverU 🎉", html);
        } catch (Exception e) {
            System.err.println("[EmailNotificationService] sendWelcomeEmail failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6.  Admin broadcast email to all paid users (manual send)
    // ─────────────────────────────────────────────────────────────────────────
    @Async
    public void broadcastToAllPaidUsers(String subject, String message) {
        try {
            List<User> paid = userRepo.findBySubscriptionStatus(User.SubscriptionStatus.PAID);
            for (User user : paid) {
                try {
                    String html = broadcastHtml(user, subject, message);
                    sendHtml(user.getEmail(), subject, html);
                } catch (Exception e) {
                    System.err.println("[EmailNotificationService] Broadcast failed for " + user.getEmail() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[EmailNotificationService] broadcastToAllPaidUsers failed: " + e.getMessage());
        }
    }

    // 7.  Notify paid users about new content added to a program
    @Async
    public void notifyPaidUsersNewContent(String programTitle, String contentTitle) {
        try {
            List<User> paid = userRepo.findBySubscriptionStatus(User.SubscriptionStatus.PAID);
            for (User user : paid) {
                try {
                    String html = newContentHtml(user, programTitle, contentTitle);
                    sendHtml(user.getEmail(), "New Content Added — ReDiscoverU", html);
                } catch (Exception e) {
                    System.err.println("[EmailNotificationService] Content notify failed for " + user.getEmail() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[EmailNotificationService] notifyPaidUsersNewContent failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void sendHtml(String to, String subject, String htmlBody) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            String senderEmail = platformSettingsService.getSenderEmail();
            String senderName  = platformSettingsService.getSenderName();
            helper.setFrom(senderEmail, senderName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("[EmailNotificationService] sendHtml failed to " + to + ": " + e.getMessage());
        }
    }

    /** One detail row in the admin notification table. */
    private String row(String label, String value) {
        return "<tr style='border-bottom:1px solid #e2e8f0;'>" +
               "<td style='padding:12px 16px;background:#f8fafc;font-size:11px;letter-spacing:1px;" +
               "text-transform:uppercase;color:#94a3b8;width:140px;'>" + label + "</td>" +
               "<td style='padding:12px 16px;font-size:14px;color:#0f172a;'>" + esc(value) + "</td>" +
               "</tr>";
    }

    private String esc(String s) {
        if (s == null) return "—";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ── Program email ──────────────────────────────────────────────
    private String programHtml(User user, Program program) {
        String firstName = user.getName().split(" ")[0];
        String desc = program.getDescription() != null ? program.getDescription() : "";
        if (desc.length() > 180) desc = desc.substring(0, 180) + "…";

        return header("New Program Available") +
               "<tr><td style='padding:36px 48px 16px;'>" +
               "<div style='font-size:10px;letter-spacing:3px;text-transform:uppercase;color:#C6A85C;margin-bottom:16px;'>Program Update</div>" +
               "<h2 style='margin:0 0 8px;font-weight:400;font-size:26px;color:#f1f5f9;'>Hello, " + esc(firstName) + "</h2>" +
               "<p style='margin:0 0 28px;color:#94a3b8;font-size:15px;line-height:1.7;'>A new program has been added to your dashboard.</p>" +
               "<div style='background:#0b1120;border:1px solid #1e293b;padding:28px 32px;margin-bottom:28px;'>" +
               "<div style='font-size:18px;color:#f1f5f9;margin-bottom:8px;'>" + esc(program.getTitle()) + "</div>" +
               (program.getTagline() != null ? "<div style='font-size:13px;color:#C6A85C;margin-bottom:12px;'>" + esc(program.getTagline()) + "</div>" : "") +
               "<div style='font-size:13px;color:#94a3b8;line-height:1.7;'>" + esc(desc) + "</div>" +
               "</div>" +
               "<p style='margin:0 0 8px;color:#64748b;font-size:13px;'>Log in to your dashboard to access this program.</p>" +
               "</td></tr>" +
               footer();
    }

    // ── Session email ──────────────────────────────────────────────
    private String sessionHtml(User user, LiveSession session, String programName) {
        String firstName = user.getName().split(" ")[0];
        String joinBtn = session.getMeetingLink() != null
                ? "<a href='" + session.getMeetingLink() + "' style='display:inline-block;background:#C6A85C;" +
                  "color:#020617;padding:14px 32px;text-decoration:none;font-size:13px;letter-spacing:1px;" +
                  "text-transform:uppercase;'>Join Session →</a>"
                : "<p style='color:#64748b;font-size:13px;'>Meeting link will be shared soon.</p>";

        return header("New Session Scheduled") +
               "<tr><td style='padding:36px 48px 16px;'>" +
               "<div style='font-size:10px;letter-spacing:3px;text-transform:uppercase;color:#C6A85C;margin-bottom:16px;'>Session Announcement</div>" +
               "<h2 style='margin:0 0 8px;font-weight:400;font-size:26px;color:#f1f5f9;'>Hello, " + esc(firstName) + "</h2>" +
               "<p style='margin:0 0 28px;color:#94a3b8;font-size:15px;line-height:1.7;'>A new live session has been scheduled. Show up with intention.</p>" +
               "<div style='background:#0b1120;border:1px solid #1e293b;padding:28px 32px;margin-bottom:28px;'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'>" +
               sessionRow("Session", session.getTitle()) +
               sessionRow("Program", programName) +
               (session.getMeetingLink() != null ? sessionRow("Link", session.getMeetingLink()) : "") +
               "</table></div>" +
               "<div style='text-align:center;'>" + joinBtn + "</div>" +
               "</td></tr>" +
               footer();
    }

    private String sessionRow(String label, String value) {
        return "<tr><td style='padding:8px 0;border-bottom:1px solid #1e293b;'>" +
               "<span style='color:#475569;font-size:11px;letter-spacing:2px;text-transform:uppercase;'>" + label + "</span><br>" +
               "<span style='color:#f1f5f9;font-size:15px;'>" + esc(value) + "</span></td></tr>";
    }

    // ── Announcement email ─────────────────────────────────────────
    private String announcementHtml(User user, String title, String message) {
        String firstName = user.getName().split(" ")[0];

        return header("Announcement") +
               "<tr><td style='padding:36px 48px 16px;'>" +
               "<div style='font-size:10px;letter-spacing:3px;text-transform:uppercase;color:#C6A85C;margin-bottom:16px;'>Announcement</div>" +
               "<h2 style='margin:0 0 8px;font-weight:400;font-size:26px;color:#f1f5f9;'>Hello, " + esc(firstName) + "</h2>" +
               "<div style='background:#0b1120;border:1px solid rgba(198,168,92,0.3);padding:28px 32px;margin:24px 0;'>" +
               "<div style='font-size:17px;color:#f1f5f9;margin-bottom:12px;'>" + esc(title) + "</div>" +
               "<div style='font-size:14px;color:#94a3b8;line-height:1.8;'>" + esc(message).replace("\n", "<br>") + "</div>" +
               "</div>" +
               "<p style='margin:0;color:#64748b;font-size:13px;'>Log in to your dashboard for more details.</p>" +
               "</td></tr>" +
               footer();
    }

    // ── Shared HTML envelope ───────────────────────────────────────
    private String header(String type) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>" +
               "<body style='margin:0;padding:0;background:#020617;font-family:Georgia,serif;'>" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='background:#020617;padding:40px 0;'>" +
               "<tr><td align='center'><table width='580' cellpadding='0' cellspacing='0' " +
               "style='background:#0b1120;border:1px solid #1e293b;'>" +
               "<tr><td><div style='height:3px;background:linear-gradient(90deg,#C6A85C,transparent);'></div></td></tr>";
    }

    private String footer() {
        String contact = platformSettingsService.getContactEmail();
        return "<tr><td style='padding:20px 48px;border-top:1px solid #1e293b;'>" +
               "<p style='margin:0;font-size:11px;color:#334155;letter-spacing:1px;text-transform:uppercase;'>" +
               "ReDiscoverU · Bengaluru, India · Jayashankar Lingaiah</p>" +
               "<p style='margin:4px 0 0;font-size:11px;color:#334155;'>" +
               "For support, contact: <a href='mailto:" + contact + "' style='color:#C6A85C;'>" + contact + "</a></p>" +
               "</td></tr></table></td></tr></table></body></html>";
    }

    // ── Welcome email HTML ────────────────────────────────────────
    private String welcomeHtml(User user) {
        String firstName = user.getName().split(" ")[0];
        return header("Welcome") +
               "<tr><td style='padding:36px 48px 16px;'>" +
               "<div style='font-size:10px;letter-spacing:3px;text-transform:uppercase;color:#C6A85C;margin-bottom:16px;'>Membership Activated</div>" +
               "<h2 style='margin:0 0 8px;font-weight:400;font-size:28px;color:#f1f5f9;'>Congratulations, " + esc(firstName) + "!</h2>" +
               "<p style='margin:0 0 28px;color:#94a3b8;font-size:15px;line-height:1.7;'>Welcome to ReDiscoverU. Your membership has been successfully activated.</p>" +
               "<div style='background:#0b1120;border:1px solid rgba(198,168,92,0.25);padding:28px 32px;margin-bottom:28px;'>" +
               "<div style='font-size:12px;letter-spacing:2px;text-transform:uppercase;color:#C6A85C;margin-bottom:16px;'>You now have lifetime access to</div>" +
               "<table width='100%' cellpadding='0' cellspacing='0'>" +
               accessRow("All self-paced programs") +
               accessRow("Recorded sessions and resources") +
               accessRow("Live Google Meet growth sessions") +
               accessRow("1-to-1 mentorship materials") +
               accessRow("All future content added to the platform") +
               "</table></div>" +
               "<p style='margin:0 0 24px;color:#94a3b8;font-size:14px;line-height:1.8;'>Your journey of growth and transformation begins today. Log in to your dashboard to start exploring.</p>" +
               "<div style='text-align:center;margin-bottom:8px;'>" +
               "<a href='http://localhost:3000/user/dashboard.html' style='display:inline-block;background:#C6A85C;" +
               "color:#020617;padding:14px 36px;text-decoration:none;font-size:13px;letter-spacing:1px;" +
               "text-transform:uppercase;font-weight:500;'>Login to Dashboard →</a></div>" +
               "</td></tr>" +
               footer();
    }

    private String accessRow(String item) {
        return "<tr><td style='padding:7px 0;border-bottom:1px solid #1e293b;'>" +
               "<span style='color:#C6A85C;margin-right:10px;font-size:12px;'>✓</span>" +
               "<span style='color:#e2e8f0;font-size:14px;'>" + esc(item) + "</span></td></tr>";
    }

    // ── Broadcast email HTML ──────────────────────────────────────
    private String broadcastHtml(User user, String subject, String message) {
        String firstName = user.getName().split(" ")[0];
        return header("Message from ReDiscoverU") +
               "<tr><td style='padding:36px 48px 16px;'>" +
               "<div style='font-size:10px;letter-spacing:3px;text-transform:uppercase;color:#C6A85C;margin-bottom:16px;'>From the Desk of Jayashankar</div>" +
               "<h2 style='margin:0 0 8px;font-weight:400;font-size:24px;color:#f1f5f9;'>Hello, " + esc(firstName) + "</h2>" +
               "<div style='background:#0b1120;border-left:3px solid #C6A85C;padding:24px 28px;margin:24px 0;'>" +
               "<div style='font-size:16px;color:#f1f5f9;margin-bottom:12px;font-weight:500;'>" + esc(subject) + "</div>" +
               "<div style='font-size:14px;color:#94a3b8;line-height:1.9;'>" + esc(message).replace("\n", "<br>") + "</div>" +
               "</div>" +
               "<p style='margin:0 0 8px;color:#64748b;font-size:12px;'>Log in to your dashboard for more details and updates.</p>" +
               "</td></tr>" +
               footer();
    }

    // ── New content email HTML ────────────────────────────────────
    private String newContentHtml(User user, String programTitle, String contentTitle) {
        String firstName = user.getName().split(" ")[0];
        return header("New Content Available") +
               "<tr><td style='padding:36px 48px 16px;'>" +
               "<div style='font-size:10px;letter-spacing:3px;text-transform:uppercase;color:#C6A85C;margin-bottom:16px;'>Content Update</div>" +
               "<h2 style='margin:0 0 8px;font-weight:400;font-size:26px;color:#f1f5f9;'>Hello, " + esc(firstName) + "</h2>" +
               "<p style='margin:0 0 28px;color:#94a3b8;font-size:15px;line-height:1.7;'>New content has been added to your program.</p>" +
               "<div style='background:#0b1120;border:1px solid #1e293b;padding:24px 32px;margin-bottom:28px;'>" +
               "<div style='font-size:11px;letter-spacing:2px;text-transform:uppercase;color:#475569;margin-bottom:6px;'>Program</div>" +
               "<div style='font-size:16px;color:#f1f5f9;margin-bottom:16px;'>" + esc(programTitle) + "</div>" +
               "<div style='font-size:11px;letter-spacing:2px;text-transform:uppercase;color:#475569;margin-bottom:6px;'>New Content</div>" +
               "<div style='font-size:16px;color:#C6A85C;'>" + esc(contentTitle) + "</div>" +
               "</div>" +
               "<p style='margin:0 0 8px;color:#64748b;font-size:13px;'>Log in to your dashboard to access the new content.</p>" +
               "</td></tr>" +
               footer();
    }

}