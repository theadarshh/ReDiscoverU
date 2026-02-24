package com.rediscoveru.service;

import com.rediscoveru.dto.*;
import com.rediscoveru.entity.User;
import com.rediscoveru.repository.UserRepository;
import com.rediscoveru.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;
    private final AuthenticationManager authManager;
    private final EmailService emailService;

    @Transactional
    public void register(RegisterRequest req) {
        if (req.getName() == null || req.getName().isBlank())
            throw new RuntimeException("Full name is required");
        if (req.getEmail() == null || req.getEmail().isBlank())
            throw new RuntimeException("Email address is required");
        if (req.getPassword() == null || req.getPassword().length() < 8)
            throw new RuntimeException("Password must be at least 8 characters");

        String email = req.getEmail().toLowerCase().trim();

        // If PENDING (unverified email), allow re-registration to resend OTP
        if (userRepo.existsByEmail(email)) {
            User existing = userRepo.findByEmail(email).orElseThrow();
            if (existing.isEnabled())
                throw new RuntimeException("An account with this email already exists. Please sign in.");
            // email not yet verified — resend OTP
            emailService.sendRegistrationOtp(existing);
            return;
        }

        User user = new User();
        user.setName(req.getName().trim());
        user.setEmail(email);
        user.setPassword(encoder.encode(req.getPassword()));
        user.setEnabled(false);
        user.setSubscriptionStatus(User.SubscriptionStatus.PENDING);
        userRepo.save(user);

        emailService.sendRegistrationOtp(user);
    }

    @Transactional
    public void verifyEmail(OtpRequest req) {
        emailService.verifyRegistrationOtp(
            req.getEmail().toLowerCase().trim(),
            req.getOtp().trim()
        );
    }

    @Transactional
    public void resendOtp(String email) {
        User user = userRepo.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        if (user.getSubscriptionStatus() == User.SubscriptionStatus.PAID && user.isEnabled())
            throw new RuntimeException("Account is already active");
        emailService.sendRegistrationOtp(user);
    }

    public AuthResponse login(LoginRequest req) {
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(
                    req.getEmail().toLowerCase().trim(), req.getPassword()));
        } catch (DisabledException e) {
            throw new RuntimeException("Please verify your email first — check your inbox for the verification code");
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid email or password");
        } catch (LockedException e) {
            throw new RuntimeException("Account is locked — please contact support");
        }

        User user = userRepo.findByEmail(req.getEmail().toLowerCase().trim()).orElseThrow();
        String token = jwt.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponse(
            token,
            user.getName(),
            user.getEmail(),
            user.getRole().name(),
            user.getSubscriptionStatus().name()  // PENDING or PAID
        );
    }

    @Transactional
    public void forgotPassword(String email) {
        try {
            emailService.sendPasswordResetOtp(email.toLowerCase().trim());
        } catch (Exception ignored) {
            // Anti-enumeration: never reveal whether email exists
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        if (req.getNewPassword() == null || req.getNewPassword().length() < 8)
            throw new RuntimeException("Password must be at least 8 characters");
        emailService.verifyPasswordResetOtp(
            req.getEmail().toLowerCase().trim(),
            req.getOtp().trim(),
            req.getNewPassword()   // plain text — EmailService encodes it
        );
    }
}
