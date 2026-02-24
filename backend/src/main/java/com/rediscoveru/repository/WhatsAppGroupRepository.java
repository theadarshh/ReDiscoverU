package com.rediscoveru.repository;

import com.rediscoveru.entity.WhatsAppGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WhatsAppGroupRepository extends JpaRepository<WhatsAppGroup, Long> {
    List<WhatsAppGroup> findAllByOrderByCreatedAtDesc();
    List<WhatsAppGroup> findByActiveTrueOrderByCreatedAtDesc();
    List<WhatsAppGroup> findByActiveTrueOrderByCreatedAtAsc();
}
