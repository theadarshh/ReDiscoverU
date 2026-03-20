package com.rediscoveru.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Single-row settings table for platform-wide configuration.
 * Row id=1 is always the active settings record.
 *
 * Email fields:
 *   contactEmail   — displayed to users as the support/contact address
 *   senderEmail    — the "From" address used in outgoing emails (must match SMTP login)
 *   senderName     — display name shown in "From" field of all emails
 *
 * SMTP password is NOT stored here — it is set via MAIL_PASSWORD env var on the server.
 * Changing SMTP host/password requires updating server env vars and restarting the app.
 */
@Entity @Table(name = "platform_settings") @Data
public class PlatformSettings {

    @Id
    private Long id = 1L;

    /** Lifetime access price — editable by admin. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal lifetimePrice = new BigDecimal("499.00");

    @Column(length = 500)
    private String platformName = "ReDiscoverU";

    /**
     * Contact email shown to users on the website and in email footers.
     * Users write to this address for payment issues, queries, subscriptions, etc.
     */
    @Column(name = "contact_email", length = 200)
    private String contactEmail = "rediscoveruadmin@gmail.com";

    /**
     * Sender email address used in the "From:" header of all outgoing emails.
     * Must match the SMTP login username configured via MAIL_USERNAME env var.
     * Changing this here changes the display only — update MAIL_USERNAME env var too
     * if you want a different SMTP account to actually send.
     */
    @Column(name = "sender_email", length = 200)
    private String senderEmail = "rediscoveruadmin@gmail.com";

    /**
     * Display name shown in the "From:" header. e.g. "ReDiscoverU" or "Jayashankar — ReDiscoverU"
     */
    @Column(name = "sender_name", length = 100)
    private String senderName = "ReDiscoverU";
}
