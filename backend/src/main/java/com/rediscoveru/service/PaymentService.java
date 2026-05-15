package com.rediscoveru.service;

import com.razorpay.RazorpayClient;
import com.razorpay.Order;
import com.rediscoveru.dto.PaymentDTO;
import com.rediscoveru.dto.PaymentInitRequest;
import com.rediscoveru.entity.*;
import com.rediscoveru.repository.*;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PaymentService
 *
 * IMPORTANT — Why we return List<PaymentDTO> instead of List<Payment>:
 *   Payment.user is @ManyToOne(fetch=LAZY). Jackson serializing a Payment
 *   outside a Hibernate session triggers LazyInitializationException, which
 *   Spring's GlobalExceptionHandler maps to HTTP 400.
 *   The mapping to PaymentDTO happens HERE (inside @Transactional), where
 *   the session is still open, so user data loads safely.
 *
 * Payment amounts come from PlatformSettings (DB), never hardcoded.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentConfigService       paymentConfigService;
    private final CouponRepository           couponRepo;
    private final PaymentRepository          paymentRepo;
    private final UserRepository             userRepo;
    private final PlatformSettingsRepository settingsRepo;
    private final EmailService               emailService;

    // ── Create order / grant free access ─────────────────────────────

    @Transactional
    public Map<String, Object> initiate(String email, PaymentInitRequest req) throws Exception {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEnabled())
            throw new RuntimeException("Please verify your email before purchasing");
        if (user.getSubscriptionStatus() == User.SubscriptionStatus.PAID)
            throw new RuntimeException("You already have lifetime access");

        // ── Always fetch price from DB — admin can change it any time ──
        PlatformSettings settings = settingsRepo.findById(1L)
                .orElseGet(PlatformSettings::new);
        BigDecimal original = settings.getLifetimePrice();
        if (original == null || original.compareTo(BigDecimal.ZERO) <= 0) {
            original = new BigDecimal("499.00");
        }

        int    discountPct = 0;
        String couponCode  = null;

        // ── Coupon validation ─────────────────────────────────────────
        if (req.getCouponCode() != null && !req.getCouponCode().isBlank()) {
            String code = req.getCouponCode().trim().toUpperCase();
            Coupon coupon = couponRepo.findByCodeWithLock(code)
                    .orElseThrow(() -> new RuntimeException("Coupon code not found"));
            if (!coupon.isActive())
                throw new RuntimeException("This coupon is no longer active");
            if (coupon.getMaxUsage() != null && coupon.getUsageCount() >= coupon.getMaxUsage())
                throw new RuntimeException("Coupon has reached its usage limit");
            discountPct = coupon.getDiscountPercentage();
            coupon.setUsageCount(coupon.getUsageCount() + 1);
            couponRepo.save(coupon);
            couponCode = code;
        }

        BigDecimal discountAmt = original
                .multiply(BigDecimal.valueOf(discountPct))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal finalAmt = original.subtract(discountAmt).max(BigDecimal.ZERO);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("originalAmount",   original);
        result.put("discountPercent",  discountPct);
        result.put("finalAmount",      finalAmt);
        result.put("currency",         "INR");

        // ── 100% coupon → grant free access immediately ───────────────
        if (finalAmt.compareTo(BigDecimal.ZERO) == 0) {
            Payment payment = new Payment();
            payment.setUser(user);
            payment.setOriginalAmount(original);
            payment.setDiscountPercentage(discountPct);
            payment.setFinalAmount(BigDecimal.ZERO);
            payment.setCouponCode(couponCode);
            payment.setPaymentStatus(Payment.PaymentStatus.FREE);
            paymentRepo.save(payment);
            grantAccess(user);
            result.put("freeAccess", true);
            result.put("message",    "Access granted via coupon");
            return result;
        }

        // ── Paid flow — create Razorpay order ─────────────────────────
        String keyId     = paymentConfigService.getKeyId();
        String keySecret = paymentConfigService.getKeySecret();

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setOriginalAmount(original);
        payment.setDiscountPercentage(discountPct);
        payment.setFinalAmount(finalAmt);
        payment.setCouponCode(couponCode);
        payment.setPaymentStatus(Payment.PaymentStatus.PENDING);

        RazorpayClient rzp = new RazorpayClient(keyId, keySecret);
        JSONObject orderReq = new JSONObject();
        // amount in paise (INR smallest unit)
        orderReq.put("amount",   finalAmt.multiply(BigDecimal.valueOf(100)).intValue());
        orderReq.put("currency", "INR");
        orderReq.put("receipt",  "rdu_" + System.currentTimeMillis());

        Order order   = rzp.orders.create(orderReq);
        String orderId = order.get("id").toString();
        payment.setRazorpayOrderId(orderId);
        paymentRepo.save(payment);

        result.put("freeAccess", false);
        result.put("orderId",    orderId);
        result.put("amount",     finalAmt.multiply(BigDecimal.valueOf(100)).intValue());
        result.put("keyId",      keyId);
        return result;
    }

    // ── Webhook ───────────────────────────────────────────────────────

    @Transactional
    public void handleWebhook(String payload, String signature) throws Exception {
        String secret   = paymentConfigService.getWebhookSecret();
        String computed = hmacSha256(payload, secret);
        if (!computed.equals(signature))
            throw new RuntimeException("Webhook signature verification failed");

        JSONObject event = new JSONObject(payload);
        if (!"payment.captured".equals(event.getString("event"))) return;

        JSONObject entity = event.getJSONObject("payload")
                .getJSONObject("payment").getJSONObject("entity");
        String razorpayOrderId   = entity.getString("order_id");
        String razorpayPaymentId = entity.getString("id");

        Payment payment = paymentRepo.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("No payment record for order: " + razorpayOrderId));
        if (payment.getPaymentStatus() == Payment.PaymentStatus.SUCCESS) return; // idempotent

        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setPaymentStatus(Payment.PaymentStatus.SUCCESS);
        paymentRepo.save(payment);
        grantAccess(payment.getUser());
    }

    // ── Queries — return DTOs (safe to serialize, no lazy proxies) ────

    /**
     * User's own payment history.
     * Mapped to DTO inside @Transactional — session is open, lazy user loads fine.
     */
    @Transactional(readOnly = true)
    public List<PaymentDTO> getUserPayments(Long userId) {
        return paymentRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * All payments — admin view.
     * Mapped to DTO inside @Transactional — session is open, lazy user loads fine.
     */
    @Transactional(readOnly = true)
    public List<PaymentDTO> getAllPayments() {
        return paymentRepo.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────────────────

    private PaymentDTO toDTO(Payment p) {
        PaymentDTO dto = new PaymentDTO();
        dto.id                 = p.getId();
        dto.originalAmount     = p.getOriginalAmount();
        dto.discountPercentage = p.getDiscountPercentage();
        dto.finalAmount        = p.getFinalAmount();
        dto.paymentStatus      = p.getPaymentStatus() != null
                                 ? p.getPaymentStatus().name() : "PENDING";
        dto.couponCode         = p.getCouponCode();
        dto.razorpayOrderId    = p.getRazorpayOrderId();
        dto.razorpayPaymentId  = p.getRazorpayPaymentId();
        dto.createdAt          = p.getCreatedAt();
        // User fields — accessed while session is open (inside @Transactional)
        if (p.getUser() != null) {
            dto.userId    = p.getUser().getId();
            dto.userName  = p.getUser().getName();
            dto.userEmail = p.getUser().getEmail();
        }
        return dto;
    }

    private void grantAccess(User user) {
        user.setSubscriptionStatus(User.SubscriptionStatus.PAID);
        userRepo.save(user);
        try { emailService.sendWelcomeEmail(user); }
        catch (Exception e) { System.err.println("[PaymentService] welcome email failed: " + e.getMessage()); }
    }

    private String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
