package com.rediscoveru.repository;

import com.rediscoveru.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {
    @Query("SELECT f FROM UploadedFile f WHERE f.program.id = :programId ORDER BY f.orderIndex ASC, f.uploadedAt ASC")
    List<UploadedFile> findByProgramIdOrderByOrderIndexAscUploadedAtAsc(@org.springframework.data.repository.query.Param("programId") Long programId);

    @Modifying
    @Query("DELETE FROM UploadedFile f WHERE f.program.id = :programId")
    void deleteByProgramId(Long programId);
}
