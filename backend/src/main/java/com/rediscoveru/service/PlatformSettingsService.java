package com.rediscoveru.service;

import com.rediscoveru.entity.PlatformSettings;
import com.rediscoveru.repository.PlatformSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * PlatformSettingsService
 * ────────────────────────
 * Single source of truth for platform-wide settings stored in the DB.
 * All services that need contactEmail / senderEmail / senderName call getSettings() here.
 */
@Service
@RequiredArgsConstructor
public class PlatformSettingsService {

    private final PlatformSettingsRepository settingsRepo;

    private static final String DEFAULT_CONTACT = "rediscoveruadmin@gmail.com";
    private static final String DEFAULT_SENDER  = "rediscoveruadmin@gmail.com";
    private static final String DEFAULT_NAME    = "ReDiscoverU";

    public PlatformSettings getSettings() {
        return settingsRepo.findById(1L).orElseGet(() -> {
            PlatformSettings s = new PlatformSettings();
            s.setId(1L);
            s.setLifetimePrice(new BigDecimal("499.00"));
            s.setContactEmail(DEFAULT_CONTACT);
            s.setSenderEmail(DEFAULT_SENDER);
            s.setSenderName(DEFAULT_NAME);
            return settingsRepo.save(s);
        });
    }

    public String getContactEmail() {
        try { String e = getSettings().getContactEmail(); return (e != null && !e.isBlank()) ? e : DEFAULT_CONTACT; }
        catch (Exception ex) { return DEFAULT_CONTACT; }
    }

    public String getSenderEmail() {
        try { String e = getSettings().getSenderEmail(); return (e != null && !e.isBlank()) ? e : DEFAULT_SENDER; }
        catch (Exception ex) { return DEFAULT_SENDER; }
    }

    public String getSenderName() {
        try { String n = getSettings().getSenderName(); return (n != null && !n.isBlank()) ? n : DEFAULT_NAME; }
        catch (Exception ex) { return DEFAULT_NAME; }
    }

    public PlatformSettings updateEmailConfig(String contactEmail, String senderEmail, String senderName) {
        PlatformSettings s = getSettings();
        if (contactEmail != null && !contactEmail.isBlank()) s.setContactEmail(contactEmail.trim());
        if (senderEmail  != null && !senderEmail.isBlank())  s.setSenderEmail(senderEmail.trim());
        if (senderName   != null && !senderName.isBlank())   s.setSenderName(senderName.trim());
        return settingsRepo.save(s);
    }
}
