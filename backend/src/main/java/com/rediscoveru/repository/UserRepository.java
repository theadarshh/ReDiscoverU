package com.rediscoveru.repository;
import com.rediscoveru.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findBySubscriptionStatus(User.SubscriptionStatus status);
    long countBySubscriptionStatus(User.SubscriptionStatus subscriptionStatus);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :from AND :to")
    long countByCreatedAtBetween(
        @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
        @org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to);

}
