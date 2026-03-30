package com.frandm.studytracker.backend.repository;

import com.frandm.studytracker.backend.model.DayNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DayNoteRepository extends JpaRepository<DayNote, Long> {
    Optional<DayNote> findByDate(LocalDate date);
}