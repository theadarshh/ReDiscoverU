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

    @Column(length = 20)
    private String phoneNumber;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.ROLE_USER;

    private boolean enabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Role { ROLE_USER, ROLE_ADMIN }
    public enum SubscriptionStatus { PENDING, PAID }
}
