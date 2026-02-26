package com.rediscoveru.controller;

import com.rediscoveru.dto.PaymentInitRequest;
import com.rediscoveru.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController @RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Creates a platform-level Razorpay order (or grants free access via 100% coupon).
     * No programId — payment unlocks the entire platform.
     */
    @PostMapping("/api/payment/order")
    public ResponseEntity<?> createOrder(@RequestBody PaymentInitRequest req,
                                         @AuthenticationPrincipal UserDetails ud) {
        try { return ResponseEntity.ok(paymentService.initiate(ud.getUsername(), req)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── Razorpay Webhook ─────────────────────────────────────────
    // Razorpay calls this directly — no JWT. Signature verified inside service.
    @PostMapping("/api/webhooks/razorpay")
    public ResponseEntity<?> razorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        try {
            paymentService.handleWebhook(payload, signature);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
}
