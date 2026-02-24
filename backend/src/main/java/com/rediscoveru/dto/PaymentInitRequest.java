package com.rediscoveru.dto;
import lombok.Data;
@Data
public class PaymentInitRequest {
    private String couponCode; // optional — programId removed (platform-level payment)
}
