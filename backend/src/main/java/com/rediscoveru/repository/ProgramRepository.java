package com.rediscoveru.repository;
import com.rediscoveru.entity.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ProgramRepository extends JpaRepository<Program, Long> {
    List<Program> findByActiveTrueOrderByCreatedAtDesc();
    List<Program> findAllByOrderByCreatedAtDesc();
}
