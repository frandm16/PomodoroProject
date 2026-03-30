package com.frandm.studytracker.backend.repository;

import com.frandm.studytracker.backend.model.TodoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TodoItemRepository extends JpaRepository<TodoItem, Long> {
    List<TodoItem> findAllByOrderByIdAsc();
    List<TodoItem> findByDateOrderByIdAsc(LocalDate date);
    List<TodoItem> findByTask_IdOrderByIdAsc(Long taskId);
    List<TodoItem> findByTask_IdAndDateOrderByIdAsc(Long taskId, LocalDate date);
}
