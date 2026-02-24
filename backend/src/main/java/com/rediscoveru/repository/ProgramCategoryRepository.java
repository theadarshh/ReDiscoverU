package com.rediscoveru.repository;

import com.rediscoveru.entity.ProgramCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProgramCategoryRepository extends JpaRepository<ProgramCategory, Long> {
    List<ProgramCategory> findByActiveTrueOrderByNameAsc();
    List<ProgramCategory> findAllByOrderByNameAsc();
    Optional<ProgramCategory> findByNameIgnoreCase(String name);
}
