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
}
