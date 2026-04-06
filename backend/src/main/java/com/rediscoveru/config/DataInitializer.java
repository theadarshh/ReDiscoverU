package com.rediscoveru.config;

import com.rediscoveru.entity.*;
import com.rediscoveru.repository.*;
import com.rediscoveru.service.AdminService;
import com.rediscoveru.service.CategoryService;
import com.rediscoveru.service.MentorService;
import com.rediscoveru.service.PaymentConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration @RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository             userRepo;
    private final ProgramRepository          programRepo;
    private final PlatformSettingsRepository settingsRepo;
    private final HomepageVideoRepository    videoRepo;
    private final MentorQuoteRepository      quoteRepo;
    private final ProgramCategoryRepository  categoryRepo;
    private final PasswordEncoder            encoder;
    private final AdminService               adminService;
    private final CategoryService            categoryService;
    private final MentorService              mentorService;
    private final PaymentConfigService       paymentConfigService;

    @Value("${admin.email}")    private String adminEmail;
    @Value("${admin.password}") private String adminPassword;
    @Value("${admin.name}")     private String adminName;

    @Bean
    public ApplicationRunner init() {
        return args -> {
            try { seedSettings();   } catch (Exception e) { System.err.println("[Seed] settings: "   + e.getMessage()); }
            try { seedAdmin();      } catch (Exception e) { System.err.println("[Seed] admin: "      + e.getMessage()); }
            try { seedCoupons();    } catch (Exception e) { System.err.println("[Seed] coupons: "    + e.getMessage()); }
            try { seedCategories(); } catch (Exception e) { System.err.println("[Seed] categories: " + e.getMessage()); }
            try { seedMentor();     } catch (Exception e) { System.err.println("[Seed] mentor: "     + e.getMessage()); }
            try { seedPrograms();   } catch (Exception e) { System.err.println("[Seed] programs: "   + e.getMessage()); }
            try { seedVideos();     } catch (Exception e) { System.err.println("[Seed] videos: "     + e.getMessage()); }
            try { seedQuote();      } catch (Exception e) { System.err.println("[Seed] quote: "      + e.getMessage()); }
            try { paymentConfigService.seedIfAbsent(); } catch (Exception e) { System.err.println("[Seed] payConfig: " + e.getMessage()); }
            System.out.println("✅ ReDiscoverU v34 — data seed complete");
        };
    }

    private void seedSettings() {
        if (settingsRepo.findById(1L).isEmpty()) {
            PlatformSettings s = new PlatformSettings();
            s.setId(1L);
            s.setLifetimePrice(new BigDecimal("499.00"));
            settingsRepo.save(s);
            System.out.println("  ✅ Platform settings: ₹499");
        }
    }

    private void seedAdmin() {
        if (userRepo.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setName(adminName);
            admin.setEmail(adminEmail);
            admin.setPassword(encoder.encode(adminPassword));
            admin.setRole(User.Role.ROLE_ADMIN);
            admin.setEnabled(true);
            admin.setSubscriptionStatus(User.SubscriptionStatus.PAID);
            userRepo.save(admin);
            System.out.println("  ✅ Admin: " + adminEmail);
        }
    }

    private void seedCoupons() { adminService.seedDefaultCoupons(); }
    private void seedCategories() { categoryService.seedDefaults(); }
    private void seedMentor() { mentorService.seedDefault(); }

    private void seedPrograms() {
        if (programRepo.count() == 0) {
            ProgramCategory morning   = categoryRepo.findByNameIgnoreCase("Morning Rituals").orElse(null);
            ProgramCategory circles   = categoryRepo.findByNameIgnoreCase("Growth Circles").orElse(null);
            ProgramCategory mentorship = categoryRepo.findByNameIgnoreCase("1-to-1 Mentorship").orElse(null);

            seedProg("Rise With Purpose",
                "A structured morning routine program to build mental clarity, discipline, and focus — one day at a time.",
                Program.ProgramType.SELF_PACED, "Lifetime access · Self-paced",
                "Begin your day with intention.", morning);
            seedProg("Growth Circles",
                "Live group mentoring sessions every Tuesday and Thursday via Google Meet. Q&A and accountability.",
                Program.ProgramType.LIVE, "Tue & Thu · Google Meet",
                "Grow with peers, guided by a mentor.", circles);
            seedProg("1-to-1 Mentorship",
                "Personalised mentorship — weekly calls, custom action plan, and priority WhatsApp access.",
                Program.ProgramType.MENTORSHIP, "4 weeks · Personalised",
                "Your journey. Your pace. Your mentor.", mentorship);
            System.out.println("  ✅ Sample programs seeded");
        }
    }

    private void seedProg(String title, String desc, Program.ProgramType type,
                          String duration, String tagline, ProgramCategory cat) {
        Program p = new Program();
        p.setTitle(title); p.setDescription(desc); p.setType(type);
        p.setDuration(duration); p.setTagline(tagline); p.setActive(true);
        p.setCategory(cat);
        programRepo.save(p);
    }

    private void seedVideos() {
        if (videoRepo.count() == 0) {
            seedVideo("Rediscover Your Purpose",         "https://youtu.be/975zQXVgu0w", 1);
            seedVideo("Building a Disciplined Morning",  "https://youtu.be/r1vrmsgQues", 2);
            seedVideo("Emotional Mastery in Daily Life", "https://youtu.be/y0NVgQ4ddB8", 3);
            System.out.println("  ✅ Homepage videos seeded");
        }
    }

    private void seedVideo(String title, String url, int order) {
        HomepageVideo v = new HomepageVideo();
        v.setTitle(title); v.setYoutubeUrl(url); v.setDisplayOrder(order); v.setActive(true);
        videoRepo.save(v);
    }

    private void seedQuote() {
        if (quoteRepo.count() == 0) {
            MentorQuote q = new MentorQuote();
            q.setQuoteText("The cost of discipline is always less than the cost of regret. The question is not whether you can — it is whether you will.");
            q.setAuthorName("Jayashankar Lingaiah");
            q.setActive(true);
            quoteRepo.save(q);
            System.out.println("  ✅ Default quote seeded");
        }
    }
}
