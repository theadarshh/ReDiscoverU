package com.rediscoveru.controller;

import com.rediscoveru.dto.*;
import com.rediscoveru.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** STEP 1 — Register: name + email + phone → sends OTP */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            authService.register(req);
            return ResponseEntity.ok(Map.of("message",
                "Registration successful. Please check your email for the verification code."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** STEP 3 — Verify OTP */
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody OtpRequest req) {
        try {
            authService.verifyEmail(req);
            return ResponseEntity.ok(Map.of("message", "Email verified. Please set your password."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** STEP 4 — Set password (after OTP verified, before payment) */
    @PostMapping("/set-password")
    public ResponseEntity<?> setPassword(@RequestBody SetPasswordRequest req) {
        try {
            authService.setPassword(req);
            return ResponseEntity.ok(Map.of("message",
                "Password set successfully. Please complete your enrolment payment."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> body) {
        try {
            authService.resendOtp(body.get("email"));
            return ResponseEntity.ok(Map.of("message", "A new verification code has been sent."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try { return ResponseEntity.ok(authService.login(req)); }
        catch (Exception e) { return ResponseEntity.status(401).body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        authService.forgotPassword(body.get("email"));
        return ResponseEntity.ok(Map.of("message",
            "If that account exists, a reset code has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req) {
        try {
            authService.resetPassword(req);
            return ResponseEntity.ok(Map.of("message",
                "Password reset successfully. You may now sign in."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
