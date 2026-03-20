package com.rediscoveru.repository;
import com.rediscoveru.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    List<Payment> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(p.finalAmount),0) FROM Payment p WHERE p.paymentStatus = 'SUCCESS'")
    BigDecimal getTotalRevenue();

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentStatus = 'SUCCESS' OR p.paymentStatus = 'FREE'")
    long countSuccessful();

    @org.springframework.data.jpa.repository.Query(
        "SELECT SUM(p.finalAmount) FROM Payment p WHERE p.paymentStatus = 'SUCCESS' " +
        "AND p.createdAt BETWEEN :from AND :to")
    java.math.BigDecimal sumRevenueByMonth(
        @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
        @org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(p) FROM Payment p WHERE (p.paymentStatus = 'SUCCESS' OR p.paymentStatus = 'FREE') " +
        "AND p.createdAt BETWEEN :from AND :to")
    long countByCreatedAtBetweenAndStatus(
        @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
        @org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to);

}
