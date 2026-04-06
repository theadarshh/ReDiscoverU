package com.rediscoveru.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Single-row settings table for platform-wide configuration.
 * Row id=1 is always the active settings record.
 */
@Entity @Table(name = "platform_settings") @Data
public class PlatformSettings {

    @Id
    private Long id = 1L;

    /** Lifetime access price — editable by admin, applied to all new payments. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal lifetimePrice = new BigDecimal("499.00");

    @Column(length = 500)
    private String platformName = "ReDiscoverU";
}
