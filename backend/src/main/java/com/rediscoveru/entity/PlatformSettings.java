package com.rediscoveru.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity @Table(name="platform_settings") @Data
public class PlatformSettings {
    @Id private Long id=1L;
    @Column(nullable=false,precision=10,scale=2)
    private BigDecimal lifetimePrice=new BigDecimal("4999.00");
    @Column(precision=10,scale=2)
    private BigDecimal launchpadPrice=new BigDecimal("499.00");
    @Column(length=500) private String platformName="ReDiscoverU";
    @Column(name="contact_email",length=200) private String contactEmail="rediscoveruadmin@gmail.com";
    @Column(name="sender_email",length=200) private String senderEmail="rediscoveruadmin@gmail.com";
    @Column(name="sender_name",length=100) private String senderName="ReDiscoverU";
}
