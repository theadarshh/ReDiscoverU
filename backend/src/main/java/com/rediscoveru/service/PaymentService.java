package com.rediscoveru.service;

import com.razorpay.RazorpayClient;
import com.razorpay.Order;
import com.rediscoveru.dto.PaymentInitRequest;
import com.rediscoveru.entity.*;
import com.rediscoveru.repository.*;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service @RequiredArgsConstructor
public class PaymentService {

    @Value("${razorpay.key.id}")         private String keyId;
    @Value("${razorpay.key.secret}")      private String keySecret;
    @Value("${razorpay.webhook.secret}")  private String webhookSecret;

    private final CouponRepository          couponRepo;
    private final PaymentRepository         paymentRepo;
    private final UserRepository            userRepo;
    private final PlatformSettingsRepository settingsRepo;

    // ── Initiate Platform Payment ──────────────────────────────────
    @Transactional
    public Map<String, Object> initiate(String email, PaymentInitRequest req) throws Exception {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEnabled())
            throw new RuntimeException("Please verify your email address before purchasing");

        if (user.getSubscriptionStatus() == User.SubscriptionStatus.PAID)
            throw new RuntimeException("You already have lifetime access to the platform");

        // Platform-level price from settings (admin-editable)
        PlatformSettings settings = settingsRepo.findById(1L)
                .orElse(new PlatformSettings());
        BigDecimal original = settings.getLifetimePrice();

        int discountPct = 0;
        String couponCode = null;

        // ── Coupon validation — backend-only ──────────────────────
        if (req.getCouponCode() != null && !req.getCouponCode().isBlank()) {
            String code = req.getCouponCode().trim().toUpperCase();
            Coupon coupon = couponRepo.findByCodeWithLock(code)
                    .orElseThrow(() -> new RuntimeException("Coupon code not found"));
            if (!coupon.isActive())
                throw new RuntimeException("This coupon is no longer active");
            if (coupon.getMaxUsage() != null && coupon.getUsageCount() >= coupon.getMaxUsage())
                throw new RuntimeException("This coupon has reached its usage limit");

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
        result.put("originalAmount", original);
        result.put("discountPercent", discountPct);
        result.put("finalAmount", finalAmt);

        // ── 100% coupon — FREE lifetime access ────────────────────
        // No Razorpay order. subscription_status set to PAID here, transactionally.
        if (finalAmt.compareTo(BigDecimal.ZERO) == 0) {
            Payment payment = new Payment();
            payment.setUser(user);
            payment.setOriginalAmount(original);
            payment.setDiscountPercentage(discountPct);
            payment.setFinalAmount(BigDecimal.ZERO);
            payment.setCouponCode(couponCode);
            payment.setPaymentStatus(Payment.PaymentStatus.FREE);
            paymentRepo.save(payment);

            grantLifetimeAccess(user);

            result.put("freeAccess", true);
            result.put("message", "Access granted via coupon");
            return result;
        }

        // ── Paid — create Razorpay order ──────────────────────────
        Payment payment = new Payment();
        payment.setUser(user);
        payment.setOriginalAmount(original);
        payment.setDiscountPercentage(discountPct);
        payment.setFinalAmount(finalAmt);
        payment.setCouponCode(couponCode);
        payment.setPaymentStatus(Payment.PaymentStatus.PENDING);

        RazorpayClient rzp = new RazorpayClient(keyId, keySecret);
        JSONObject orderReq = new JSONObject();
        orderReq.put("amount", finalAmt.multiply(BigDecimal.valueOf(100)).intValue());
        orderReq.put("currency", "INR");
        orderReq.put("receipt", "rdu_" + System.currentTimeMillis());

        Order order = rzp.orders.create(orderReq);
        String orderId = order.get("id").toString();

        payment.setRazorpayOrderId(orderId);
        paymentRepo.save(payment);

        result.put("freeAccess", false);
        result.put("orderId", orderId);
        result.put("amount", finalAmt.multiply(BigDecimal.valueOf(100)).intValue());
        result.put("currency", "INR");
        result.put("keyId", keyId);
        return result;
    }

    // ── Razorpay Webhook — payment.captured ───────────────────────
    // subscription_status = PAID set ONLY after HMAC-SHA256 verification.
    @Transactional
    public void handleWebhook(String payload, String signature) throws Exception {
        String computed = hmacSha256(payload, webhookSecret);
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

        grantLifetimeAccess(payment.getUser());
    }

    // ── Queries ───────────────────────────────────────────────────
    public List<Payment> getUserPayments(Long userId) {
        return paymentRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Payment> getAllPayments() {
        return paymentRepo.findAllByOrderByCreatedAtDesc();
    }

    // ── Private helpers ───────────────────────────────────────────

    /**
     * Grants platform-wide lifetime access.
     * Called ONLY from:
     *   1. handleWebhook() — after HMAC-SHA256 Razorpay verification
     *   2. initiate()      — when finalAmount = 0 (100% coupon, e.g. JAYCIRCLE)
     */
    private void grantLifetimeAccess(User user) {
        user.setSubscriptionStatus(User.SubscriptionStatus.PAID);
        userRepo.save(user);
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
