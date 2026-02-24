package com.rediscoveru.service;

import com.rediscoveru.entity.*;
import com.rediscoveru.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service @RequiredArgsConstructor
public class AdminService {

    private final UserRepository    userRepo;
    private final CouponRepository  couponRepo;
    private final PaymentRepository paymentRepo;
    private final ProgramRepository programRepo;

    public List<User> getAllUsers() { return userRepo.findAll(); }

    public Map<String, Object> getStats() {
        Map<String, Object> s = new LinkedHashMap<>();
        try { s.put("totalUsers",    userRepo.count()); } catch (Exception e) { s.put("totalUsers", 0); }
        try { s.put("paidUsers",     userRepo.countBySubscriptionStatus(User.SubscriptionStatus.PAID)); } catch (Exception e) { s.put("paidUsers", 0); }
        try { s.put("pendingUsers",  userRepo.countBySubscriptionStatus(User.SubscriptionStatus.PENDING)); } catch (Exception e) { s.put("pendingUsers", 0); }
        try { s.put("totalPrograms", programRepo.count()); } catch (Exception e) { s.put("totalPrograms", 0); }
        try {
            java.math.BigDecimal rev = paymentRepo.getTotalRevenue();
            s.put("totalRevenue", rev != null ? rev : java.math.BigDecimal.ZERO);
        } catch (Exception e) { s.put("totalRevenue", 0); }
        try { s.put("successfulPayments", paymentRepo.countSuccessful()); } catch (Exception e) { s.put("successfulPayments", 0); }
        try { s.put("totalPayments",      paymentRepo.count()); } catch (Exception e) { s.put("totalPayments", 0); }
        return s;
    }

    /** Admin can manually activate a user's subscription (e.g. offline payment) */
    public void activateUser(Long userId) {
        User u = userRepo.findById(userId).orElseThrow();
        u.setEnabled(true);
        u.setSubscriptionStatus(User.SubscriptionStatus.PAID);
        userRepo.save(u);
    }

    /** Enable or disable login for a user */
    public void toggleUserEnabled(Long userId) {
        User u = userRepo.findById(userId).orElseThrow();
        u.setEnabled(!u.isEnabled());
        userRepo.save(u);
    }

    public List<Coupon> getCoupons() { return couponRepo.findAllByOrderByIdDesc(); }

    public Coupon createCoupon(Coupon c) {
        c.setCode(c.getCode().toUpperCase().trim());
        if (couponRepo.findByCode(c.getCode()).isPresent())
            throw new RuntimeException("A coupon with this code already exists");
        return couponRepo.save(c);
    }

    public void toggleCoupon(Long id) {
        Coupon c = couponRepo.findById(id).orElseThrow();
        c.setActive(!c.isActive());
        couponRepo.save(c);
    }

    public void seedDefaultCoupons() {
        seed("FIRST10",   100, 10);
        seed("OFF50",      50, 10);
        seed("OFF25",      25, null);
        seed("JAYCIRCLE", 100, null);  // Unlimited 100% coupon
    }

    private void seed(String code, int pct, Integer max) {
        if (couponRepo.findByCode(code).isEmpty()) {
            Coupon c = new Coupon();
            c.setCode(code);
            c.setDiscountPercentage(pct);
            c.setMaxUsage(max);
            couponRepo.save(c);
        }
    }
}
