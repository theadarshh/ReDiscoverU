package com.rediscoveru.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "payments") @Data
public class Payment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler","password"})
    private User user;

    @Column(precision = 10, scale = 2)
    private BigDecimal originalAmount;

    private int discountPercentage = 0;

    @Column(precision = 10, scale = 2)
    private BigDecimal finalAmount;

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String couponCode;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum PaymentStatus {
        PENDING,  // Razorpay order created, awaiting capture
        SUCCESS,  // Webhook confirmed — Razorpay payment captured
        FREE,     // 100% coupon — no Razorpay involved
        FAILED    // Payment failed
    }
}
