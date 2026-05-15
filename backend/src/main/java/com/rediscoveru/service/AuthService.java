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

    private final UserRepository     userRepo;
    private final PasswordEncoder    encoder;
    private final JwtUtil            jwt;
    private final AuthenticationManager authManager;
    private final EmailService       emailService;

    /** STEP 1 — name + email + phone → OTP */
    @Transactional
    public void register(RegisterRequest req) {
        if (req.getName() == null || req.getName().isBlank())
            throw new RuntimeException("Full name is required");
        if (req.getEmail() == null || req.getEmail().isBlank())
            throw new RuntimeException("Email address is required");

        String email = req.getEmail().toLowerCase().trim();

        if (userRepo.existsByEmail(email)) {
            User existing = userRepo.findByEmail(email).orElseThrow();
            if (existing.isEnabled() && existing.getPassword() != null
                    && !existing.getPassword().contains("__UNSET__"))
                throw new RuntimeException("Account already exists. Please sign in.");
            // Re-register: update name/phone and resend OTP
            existing.setName(req.getName().trim());
            if (req.getPhoneNumber() != null && !req.getPhoneNumber().isBlank())
                existing.setPhoneNumber(req.getPhoneNumber().trim());
            userRepo.save(existing);
            emailService.sendRegistrationOtp(existing);
            return;
        }

        User user = new User();
        user.setName(req.getName().trim());
        user.setEmail(email);
        if (req.getPhoneNumber() != null && !req.getPhoneNumber().isBlank())
            user.setPhoneNumber(req.getPhoneNumber().trim());
        user.setPassword(encoder.encode("__UNSET__"));
        user.setEnabled(false);
        user.setSubscriptionStatus(User.SubscriptionStatus.PENDING);
        userRepo.save(user);

        emailService.sendRegistrationOtp(user);
        try { emailService.notifyAdminNewRegistration(user); } catch (Exception ignored) {}
    }

    /** STEP 2 — verify OTP */
    @Transactional
    public void verifyEmail(OtpRequest req) {
        emailService.verifyRegistrationOtp(
                req.getEmail().toLowerCase().trim(),
                req.getOtp().trim());
    }

    /** STEP 3 — set password */
    @Transactional
    public void setPassword(SetPasswordRequest req) {
        if (req.getPassword() == null || req.getPassword().length() < 8)
            throw new RuntimeException("Password must be at least 8 characters");
        User user = userRepo.findByEmail(req.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        if (!user.isEnabled())
            throw new RuntimeException("Please verify your email first");
        user.setPassword(encoder.encode(req.getPassword()));
        userRepo.save(user);
    }

    /** Resend OTP */
    @Transactional
    public void resendOtp(String email) {
        User user = userRepo.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        emailService.sendRegistrationOtp(user);
    }

    /** Login */
    public AuthResponse login(LoginRequest req) {
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(
                    req.getEmail().toLowerCase().trim(), req.getPassword()));
        } catch (DisabledException e) {
            throw new RuntimeException("Please verify your email first");
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid email or password");
        } catch (LockedException e) {
            throw new RuntimeException("Account is locked — contact support");
        }
        User user = userRepo.findByEmail(req.getEmail().toLowerCase().trim()).orElseThrow();
        String token = jwt.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getName(), user.getEmail(),
                user.getRole().name(), user.getSubscriptionStatus().name());
    }

    /** Forgot password */
    @Transactional
    public void forgotPassword(String email) {
        try { emailService.sendPasswordResetOtp(email.toLowerCase().trim()); }
        catch (Exception ignored) {} // anti-enumeration
    }

    /** Reset password */
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        if (req.getNewPassword() == null || req.getNewPassword().length() < 8)
            throw new RuntimeException("Password must be at least 8 characters");
        emailService.verifyPasswordResetOtp(
                req.getEmail().toLowerCase().trim(),
                req.getOtp().trim(),
                req.getNewPassword());
    }
}
