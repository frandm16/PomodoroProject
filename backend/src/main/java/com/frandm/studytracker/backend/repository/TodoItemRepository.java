package com.frandm.studytracker.backend.repository;

import com.frandm.studytracker.backend.model.TodoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TodoItemRepository extends JpaRepository<TodoItem, Long> {
    @Query("SELECT t FROM TodoItem t WHERE " +
            "(:taskId IS NULL OR t.task.id = :taskId) AND " +
            "(:date IS NULL OR t.date = :date) " +
            "ORDER BY t.id ASC")
    List<TodoItem> findFiltered(
            @Param("taskId") Long taskId,
            @Param("date") LocalDate date
    );
}
