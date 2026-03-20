package com.rediscoveru.repository;
import com.rediscoveru.entity.MentorQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
public interface MentorQuoteRepository extends JpaRepository<MentorQuote, Long> {
    Optional<MentorQuote> findFirstByActiveTrueOrderByCreatedAtDesc();
    List<MentorQuote> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE MentorQuote q SET q.active = false WHERE q.active = true")
    void deactivateAll();
}
