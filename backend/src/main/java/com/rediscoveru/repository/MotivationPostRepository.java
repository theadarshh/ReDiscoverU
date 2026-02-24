package com.rediscoveru.repository;
import com.rediscoveru.entity.MotivationPost;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MotivationPostRepository extends JpaRepository<MotivationPost, Long> {
    List<MotivationPost> findByActiveTrueOrderByCreatedAtDesc();
    List<MotivationPost> findAllByOrderByCreatedAtDesc();
}
