package com.rediscoveru.service;

import com.rediscoveru.entity.PaymentConfig;
import com.rediscoveru.repository.PaymentConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentConfigService {

    private final PaymentConfigRepository configRepo;

    @Value("${jwt.secret:ReDiscoverU_ChangeMe_LongSecretKey_Jayashankar2024}")
    private String encryptionSeed;

    @Value("${razorpay.key.id:rzp_test_placeholder}")
    private String envKeyId;

    @Value("${razorpay.key.secret:placeholder_secret}")
    private String envKeySecret;

    @Value("${razorpay.webhook.secret:placeholder_webhook}")
    private String envWebhookSecret;

    // ── Used by PaymentService ──────────────────────────────────
    public String getKeyId() {
        PaymentConfig cfg = load();
        if (cfg.isEnabled() && !cfg.getKeyId().isBlank()) return cfg.getKeyId();
        return envKeyId;
    }

    public String getKeySecret() {
        PaymentConfig cfg = load();
        if (cfg.isEnabled() && !cfg.getKeySecret().isBlank()) {
            try { return decrypt(cfg.getKeySecret()); }
            catch (Exception e) { System.err.println("[PayCfg] decrypt keySecret: " + e.getMessage()); }
        }
        return envKeySecret;
    }

    public String getWebhookSecret() {
        PaymentConfig cfg = load();
        if (cfg.isEnabled() && !cfg.getWebhookSecret().isBlank()) {
            try { return decrypt(cfg.getWebhookSecret()); }
            catch (Exception e) { System.err.println("[PayCfg] decrypt webhook: " + e.getMessage()); }
        }
        return envWebhookSecret;
    }

    // ── Admin view (never exposes secrets) ─────────────────────
    public Map<String, Object> getAdminView() {
        PaymentConfig cfg = load();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("keyId",        cfg.getKeyId());
        m.put("hasKeySecret", !cfg.getKeySecret().isBlank());
        m.put("hasWebhook",   !cfg.getWebhookSecret().isBlank());
        m.put("enabled",      cfg.isEnabled());
        m.put("updatedAt",    cfg.getUpdatedAt());
        return m;
    }

    // ── Admin save ──────────────────────────────────────────────
    public Map<String, Object> save(String keyId, String keySecret,
                                    String webhookSecret, boolean enabled) {
        PaymentConfig cfg = load();
        if (keyId != null && !keyId.isBlank()) cfg.setKeyId(keyId.trim());
        if (keySecret != null && !keySecret.isBlank()) {
            try { cfg.setKeySecret(encrypt(keySecret.trim())); }
            catch (Exception e) { throw new RuntimeException("Failed to encrypt key secret"); }
        }
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            try { cfg.setWebhookSecret(encrypt(webhookSecret.trim())); }
            catch (Exception e) { throw new RuntimeException("Failed to encrypt webhook secret"); }
        }
        cfg.setEnabled(enabled);
        cfg.setUpdatedAt(LocalDateTime.now());
        configRepo.save(cfg);
        return getAdminView();
    }

    // ── Seed on startup ─────────────────────────────────────────
    public void seedIfAbsent() {
        if (configRepo.findById(1L).isEmpty()) {
            PaymentConfig cfg = new PaymentConfig();
            cfg.setId(1L);
            configRepo.save(cfg);
            System.out.println("✅ payment_config seeded");
        }
    }

    // ── Private helpers ─────────────────────────────────────────
    private PaymentConfig load() {
        return configRepo.findById(1L).orElseGet(PaymentConfig::new);
    }

    private SecretKeySpec buildKey() throws Exception {
        byte[] raw = MessageDigest.getInstance("SHA-256")
                .digest(encryptionSeed.getBytes("UTF-8"));
        return new SecretKeySpec(raw, "AES");
    }

    private String encrypt(String plain) throws Exception {
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, buildKey());
        return Base64.getEncoder().encodeToString(c.doFinal(plain.getBytes("UTF-8")));
    }

    private String decrypt(String b64) throws Exception {
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, buildKey());
        return new String(c.doFinal(Base64.getDecoder().decode(b64)), "UTF-8");
    }
}
