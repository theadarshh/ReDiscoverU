package com.rediscoveru.repository;
import com.rediscoveru.entity.HomepageVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
public interface HomepageVideoRepository extends JpaRepository<HomepageVideo, Long> {

    @Query("SELECT v FROM HomepageVideo v WHERE v.active = true ORDER BY v.displayOrder ASC, v.createdAt ASC")
    List<HomepageVideo> findByActiveTrueOrderByDisplayOrderAscCreatedAtAsc();

    @Query("SELECT v FROM HomepageVideo v ORDER BY v.displayOrder ASC, v.createdAt ASC")
    List<HomepageVideo> findAllByOrderByDisplayOrderAscCreatedAtAsc();
}
