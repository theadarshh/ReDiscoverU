package com.rediscoveru.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PaymentDTO — safe serializable representation of a payment.
 *
 * WHY THIS EXISTS:
 * The Payment entity has a @ManyToOne(fetch=LAZY) User field.
 * When Jackson tries to serialize Payment outside a Hibernate session,
 * accessing the lazy User proxy throws LazyInitializationException,
 * which Spring's GlobalExceptionHandler converts to HTTP 400.
 *
 * This DTO is built inside the service (while the transaction is still open),
 * so all data is already loaded into plain Java fields — safe to serialize anywhere.
 */
public class PaymentDTO {

    public Long          id;
    public BigDecimal    originalAmount;
    public int           discountPercentage;
    public BigDecimal    finalAmount;
    public String        paymentStatus;      // "PENDING" | "SUCCESS" | "FREE" | "FAILED"
    public String        couponCode;
    public String        razorpayOrderId;
    public String        razorpayPaymentId;
    public LocalDateTime createdAt;

    // User fields — flattened (no nested object, no lazy proxy)
    public Long          userId;
    public String        userName;
    public String        userEmail;

    // Convenience alias for frontend compatibility
    public BigDecimal getAmount() { return finalAmount; }
    public String     getStatus() { return paymentStatus; }
}
