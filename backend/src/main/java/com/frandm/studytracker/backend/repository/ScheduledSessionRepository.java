package com.frandm.studytracker.backend.repository;

import com.frandm.studytracker.backend.model.ScheduledSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledSessionRepository extends JpaRepository<ScheduledSession, Long> {

    @Query("SELECT s FROM ScheduledSession s WHERE " +
            "s.startDate BETWEEN :start AND :end")
    List<ScheduledSession> findByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
