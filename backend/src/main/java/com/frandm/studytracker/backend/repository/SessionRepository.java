package com.frandm.studytracker.backend.repository;

import com.frandm.studytracker.backend.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    @Query("SELECT s FROM Session s WHERE " +
            "(:tag IS NULL OR s.task.tag.name = :tag) AND " +
            "(:task IS NULL OR s.task.name = :task) " +
            "ORDER BY s.startDate DESC")
    Page<Session> findFiltered(
            @Param("tag") String tag,
            @Param("task") String task,
            Pageable pageable
    );

    List<Session> findByTask_Tag_NameOrderByStartDateDesc(String tagName);

    @Query("SELECT s FROM Session s WHERE s.startDate BETWEEN :start AND :end")
    List<Session> findByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
