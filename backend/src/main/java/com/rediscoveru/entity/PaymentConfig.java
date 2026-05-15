package com.rediscoveru.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity @Table(name = "payment_config") @Data
public class PaymentConfig {

    @Id
    private Long id = 1L;

    @Column(name = "razorpay_key_id", length = 120)
    private String keyId = "";

    @Column(name = "razorpay_key_secret", length = 512)
    private String keySecret = "";

    @Column(name = "razorpay_webhook_secret", length = 512)
    private String webhookSecret = "";

    @Column(name = "is_enabled")
    private boolean enabled = false;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
