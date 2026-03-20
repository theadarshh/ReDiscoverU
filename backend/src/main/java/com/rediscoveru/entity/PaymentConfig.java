package com.rediscoveru.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Stores payment gateway credentials per provider.
 * keySecret and webhookSecret are stored AES-256 encrypted.
 * Only keyId is safe to expose to the frontend.
 *
 * Extensible: add new providers by inserting additional rows.
 */
@Entity
@Table(name = "payment_config",
       uniqueConstraints = @UniqueConstraint(columnNames = "provider"))
@Data
public class PaymentConfig {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g. "razorpay" — unique per provider */
    @Column(nullable = false, length = 50)
    private String provider;

    /** Razorpay Key ID — safe to send to frontend */
    @Column(name = "key_id", length = 120)
    private String keyId;

    /** Razorpay Key Secret — stored AES-256 encrypted, NEVER sent to frontend */
    @Column(name = "key_secret", length = 512)
    private String keySecret;

    /** Webhook secret — stored AES-256 encrypted */
    @Column(name = "webhook_secret", length = 512)
    private String webhookSecret;

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
