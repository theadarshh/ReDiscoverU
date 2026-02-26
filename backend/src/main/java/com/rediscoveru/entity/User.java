package com.rediscoveru.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity @Table(name = "users") @Data
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.ROLE_USER;

    /**
     * Controls Spring Security authentication.
     * false = email not yet verified (cannot login at all)
     * true  = email verified (can login; access gated by subscriptionStatus)
     */
    private boolean enabled = false;

    /**
     * DB column: subscription_status enum('PENDING','PAID')
     *
     * UNVERIFIED — registered, OTP not confirmed. Stored as column default; not an enum value.
     *              Spring Security blocks login via enabled=false.
     * PENDING    — OTP verified, awaiting payment
     * PAID       — payment confirmed via webhook OR 100% coupon. Full access granted.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Role { ROLE_USER, ROLE_ADMIN }

    public enum SubscriptionStatus {
        PENDING,   // OTP done (or not yet), awaiting payment
        PAID       // payment confirmed — full access
    }
}
