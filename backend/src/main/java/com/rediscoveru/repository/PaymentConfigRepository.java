package com.rediscoveru.repository;

import com.rediscoveru.entity.PaymentConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentConfigRepository extends JpaRepository<PaymentConfig, Long> {

    Optional<PaymentConfig> findByProvider(String provider);

    Optional<PaymentConfig> findByProviderAndIsEnabledTrue(String provider);
}
