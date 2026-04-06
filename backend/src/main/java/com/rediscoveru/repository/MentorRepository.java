package com.rediscoveru.repository;

import com.rediscoveru.entity.Mentor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MentorRepository extends JpaRepository<Mentor, Long> {
    List<Mentor> findByDeletedFalseOrderByDisplayOrderAscCreatedAtAsc();
    List<Mentor> findByActiveTrueAndDeletedFalseOrderByDisplayOrderAscCreatedAtAsc();
}
