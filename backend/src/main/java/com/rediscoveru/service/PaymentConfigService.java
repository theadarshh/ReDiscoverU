package com.rediscoveru.service;

import com.rediscoveru.entity.PaymentConfig;
import com.rediscoveru.repository.PaymentConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * PaymentConfigService
 * ─────────────────────
 * Single responsibility: manage gateway credentials stored in payment_config.
 *
 * Security model:
 *   - keySecret and webhookSecret are AES-256-CBC encrypted at rest.
 *   - The encryption key is stored only in application.properties (env var in production).
 *   - keyId is stored plain — it is intentionally public (used in Razorpay checkout JS).
 *   - Admin API never returns keySecret or webhookSecret — only keyId and status.
 *
 * Extensible: supports multiple providers. Pass provider = "razorpay" (or future providers).
 */
@Service
@RequiredArgsConstructor
public class PaymentConfigService {

    private static final String PROVIDER_RAZORPAY = "razorpay";
    private static final String ALGO = "AES/CBC/PKCS5Padding";

    private final PaymentConfigRepository configRepo;

    @Value("${payment.config.encryption.key:ReDiscoverU_PayCfg_DefaultKey_2026}")
    private String encryptionKey;

    // ─────────────────────────────────────────────────────────────────────────
    // Public: fetch active Razorpay config (used by PaymentService)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the active Razorpay config with secrets decrypted.
     * Throws RuntimeException if not configured or disabled.
     */
    public PaymentConfig getActiveRazorpayConfig() {
        PaymentConfig cfg = configRepo
                .findByProviderAndIsEnabledTrue(PROVIDER_RAZORPAY)
                .orElseThrow(() -> new RuntimeException(
                        "Payment gateway is not configured. Please ask the administrator to set up Razorpay keys."));

        if (cfg.getKeyId() == null || cfg.getKeyId().isBlank())
            throw new RuntimeException("Payment gateway is not configured. Key ID is missing.");

        if (cfg.getKeySecret() == null || cfg.getKeySecret().isBlank())
            throw new RuntimeException("Payment gateway is not configured. Key Secret is missing.");

        // Decrypt secrets before returning
        PaymentConfig decrypted = new PaymentConfig();
        decrypted.setId(cfg.getId());
        decrypted.setProvider(cfg.getProvider());
        decrypted.setKeyId(cfg.getKeyId());
        decrypted.setEnabled(cfg.isEnabled());
        decrypted.setCreatedAt(cfg.getCreatedAt());
        decrypted.setUpdatedAt(cfg.getUpdatedAt());

        try {
            decrypted.setKeySecret(decrypt(cfg.getKeySecret()));
        } catch (Exception e) {
            throw new RuntimeException("Payment gateway configuration is corrupted. Please re-save the keys.");
        }

        try {
            if (cfg.getWebhookSecret() != null && !cfg.getWebhookSecret().isBlank())
                decrypted.setWebhookSecret(decrypt(cfg.getWebhookSecret()));
        } catch (Exception e) {
            // webhook secret decrypt failure is non-fatal for order creation
            System.err.println("[PaymentConfigService] Webhook secret decrypt failed: " + e.getMessage());
        }

        return decrypted;
    }

    /**
     * Returns the active webhook secret (decrypted), or null if not set.
     * Used only by PaymentService.handleWebhook().
     */
    public String getActiveWebhookSecret() {
        return configRepo.findByProviderAndIsEnabledTrue(PROVIDER_RAZORPAY)
                .map(cfg -> {
                    if (cfg.getWebhookSecret() == null || cfg.getWebhookSecret().isBlank()) return null;
                    try { return decrypt(cfg.getWebhookSecret()); }
                    catch (Exception e) {
                        System.err.println("[PaymentConfigService] Webhook decrypt failed: " + e.getMessage());
                        return null;
                    }
                })
                .orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Admin: CRUD — secrets encrypted before storage, never returned in plain
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns config for admin UI — NEVER includes keySecret or webhookSecret.
     * Only keyId, isEnabled, provider, timestamps.
     */
    public Map<String, Object> getAdminSafeConfig() {
        Optional<PaymentConfig> opt = configRepo.findByProvider(PROVIDER_RAZORPAY);
        Map<String, Object> result = new LinkedHashMap<>();
        if (opt.isEmpty()) {
            result.put("configured", false);
            result.put("isEnabled", false);
            result.put("keyId", "");
            result.put("hasKeySecret", false);
            result.put("hasWebhookSecret", false);
            result.put("provider", PROVIDER_RAZORPAY);
            return result;
        }
        PaymentConfig cfg = opt.get();
        result.put("configured", true);
        result.put("id", cfg.getId());
        result.put("provider", cfg.getProvider());
        result.put("keyId", cfg.getKeyId() != null ? cfg.getKeyId() : "");
        result.put("isEnabled", cfg.isEnabled());
        result.put("hasKeySecret", cfg.getKeySecret() != null && !cfg.getKeySecret().isBlank());
        result.put("hasWebhookSecret", cfg.getWebhookSecret() != null && !cfg.getWebhookSecret().isBlank());
        result.put("updatedAt", cfg.getUpdatedAt());
        return result;
    }

    /**
     * Save or update Razorpay credentials from admin form.
     * Encrypts keySecret and webhookSecret before persisting.
     * keyId is stored plain (it's public).
     *
     * @param keyId         plain Razorpay Key ID
     * @param keySecret     plain Razorpay Key Secret — encrypted before save
     * @param webhookSecret plain Webhook Secret — encrypted before save (nullable)
     * @param enabled       whether to enable the gateway immediately
     */
    /**
     * Save or update Razorpay credentials.
     * If keySecret is null/blank AND a record already exists → keep the existing encrypted secret.
     * If keySecret is null/blank AND no record exists → throw error (first-time setup requires it).
     * webhookSecret null/blank → clear the stored webhook secret.
     */
    public Map<String, Object> saveConfig(String keyId, String keySecret,
                                          String webhookSecret, boolean enabled) {
        if (keyId == null || keyId.isBlank())
            throw new RuntimeException("Key ID is required");

        boolean isNewRecord = configRepo.findByProvider(PROVIDER_RAZORPAY).isEmpty();

        if ((keySecret == null || keySecret.isBlank()) && isNewRecord)
            throw new RuntimeException("Key Secret is required for first-time setup");

        PaymentConfig cfg = configRepo.findByProvider(PROVIDER_RAZORPAY)
                .orElseGet(() -> {
                    PaymentConfig n = new PaymentConfig();
                    n.setProvider(PROVIDER_RAZORPAY);
                    return n;
                });

        cfg.setKeyId(keyId.trim());

        // Only update keySecret if a new value was provided
        if (keySecret != null && !keySecret.isBlank()) {
            try {
                cfg.setKeySecret(encrypt(keySecret.trim()));
            } catch (Exception e) {
                throw new RuntimeException("Key Secret encryption failed: " + e.getMessage());
            }
        }
        // webhook secret: null/blank = clear; non-blank = encrypt and store
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            try {
                cfg.setWebhookSecret(encrypt(webhookSecret.trim()));
            } catch (Exception e) {
                throw new RuntimeException("Webhook secret encryption failed: " + e.getMessage());
            }
        } else {
            cfg.setWebhookSecret(null);
        }
        cfg.setEnabled(enabled);
        configRepo.save(cfg);

        return getAdminSafeConfig();
    }

    /**
     * Toggle enabled/disabled without changing credentials.
     */
    public Map<String, Object> toggleEnabled(boolean enabled) {
        PaymentConfig cfg = configRepo.findByProvider(PROVIDER_RAZORPAY)
                .orElseThrow(() -> new RuntimeException("No Razorpay config found. Please save credentials first."));
        cfg.setEnabled(enabled);
        configRepo.save(cfg);
        return getAdminSafeConfig();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Called by DataInitializer on first boot
    // ─────────────────────────────────────────────────────────────────────────

    public void seedPlaceholder() {
        if (configRepo.findByProvider(PROVIDER_RAZORPAY).isEmpty()) {
            PaymentConfig cfg = new PaymentConfig();
            cfg.setProvider(PROVIDER_RAZORPAY);
            cfg.setKeyId("");
            cfg.setKeySecret("");
            cfg.setWebhookSecret("");
            cfg.setEnabled(false);
            configRepo.save(cfg);
            System.out.println("✅ Payment config — placeholder created (disabled)");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AES-256-CBC encrypt / decrypt — uses the env-configured encryption key
    // ─────────────────────────────────────────────────────────────────────────

    private SecretKeySpec buildKey() throws Exception {
        byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                .digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, "AES");
    }

    private String encrypt(String plain) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGO);
        // Deterministic IV derived from key — predictable but sufficient for secrets at rest
        byte[] iv = Arrays.copyOf(
                MessageDigest.getInstance("MD5").digest(encryptionKey.getBytes(StandardCharsets.UTF_8)),
                16);
        cipher.init(Cipher.ENCRYPT_MODE, buildKey(), new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String decrypt(String base64Cipher) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGO);
        byte[] iv = Arrays.copyOf(
                MessageDigest.getInstance("MD5").digest(encryptionKey.getBytes(StandardCharsets.UTF_8)),
                16);
        cipher.init(Cipher.DECRYPT_MODE, buildKey(), new IvParameterSpec(iv));
        byte[] decoded = Base64.getDecoder().decode(base64Cipher);
        return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
    }
}
