package com.rediscoveru.repository;

import com.rediscoveru.entity.UserNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserNoteRepository extends JpaRepository<UserNote, Long> {

    List<UserNote> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<UserNote> findByUserIdAndProgramIdOrderByUpdatedAtDesc(Long userId, Long programId);

    List<UserNote> findByUserIdAndContentIdAndContentTypeOrderByCreatedAtAsc(
        Long userId, Long contentId, String contentType);

    /** Count notes per program for dashboard summary */
    @Query("SELECT un.program.id, COUNT(un) FROM UserNote un WHERE un.user.id = :uid GROUP BY un.program.id")
    List<Object[]> countByProgram(@Param("uid") Long userId);

    Optional<UserNote> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
