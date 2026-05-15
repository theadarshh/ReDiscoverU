package com.rediscoveru.repository;
import com.rediscoveru.entity.ProgramContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface ProgramContentRepository extends JpaRepository<ProgramContent, Long> {

    List<ProgramContent> findByProgramIdOrderByOrderIndexAsc(Long programId);

    List<ProgramContent> findByProgramIdAndContentTypeOrderByOrderIndexAsc(
        Long programId, ProgramContent.ContentType contentType);

    @Query("SELECT c FROM ProgramContent c WHERE c.contentType = :type AND c.program.active = true ORDER BY c.program.id ASC, c.orderIndex ASC")
    List<ProgramContent> findByContentTypeAndProgramActiveTrueOrderByProgramIdAscOrderIndexAsc(
        @Param("type") ProgramContent.ContentType contentType);

    void deleteByProgramId(Long programId);
}
