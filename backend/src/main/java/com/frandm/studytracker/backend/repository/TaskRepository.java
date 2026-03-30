package com.frandm.studytracker.backend.repository;

import com.frandm.studytracker.backend.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByTag_NameOrderByNameAsc(String tagName);
    Optional<Task> findByTag_IdAndName(Long tagId, String name);
    Optional<Task> findByIdAndTag_Name(Long id, String tagName);
}
