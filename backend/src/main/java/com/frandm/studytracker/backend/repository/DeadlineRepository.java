package com.frandm.studytracker.backend.repository;

import com.frandm.studytracker.backend.model.Deadline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DeadlineRepository extends JpaRepository<Deadline, Long> {

    @Query("SELECT d FROM Deadline d WHERE d.dueDate BETWEEN :start AND :end ORDER BY d.dueDate ASC")
    List<Deadline> findByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}