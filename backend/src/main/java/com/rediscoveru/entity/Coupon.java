package com.rediscoveru.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity @Table(name = "coupons") @Data
public class Coupon {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    private int usageCount = 0;

    private Integer maxUsage; // null = unlimited

    private int discountPercentage;

    private boolean active = true;

    @Version
    private Long version;
}
