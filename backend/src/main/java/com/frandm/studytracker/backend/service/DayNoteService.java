package com.frandm.studytracker.backend.service;

import com.frandm.studytracker.backend.model.DayNote;
import com.frandm.studytracker.backend.repository.DayNoteRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class DayNoteService {

    private final DayNoteRepository dayNoteRepository;

    public DayNoteService(DayNoteRepository dayNoteRepository) {
        this.dayNoteRepository = dayNoteRepository;
    }

    public DayNote getOrEmpty(LocalDate date) {
        return dayNoteRepository.findByDate(date).orElseGet(() -> {
            DayNote empty = new DayNote();
            empty.setDate(date);
            empty.setContent("");
            return empty;
        });
    }

    public DayNote save(LocalDate date, String content) {
        DayNote note = dayNoteRepository.findByDate(date).orElseGet(() -> {
            DayNote n = new DayNote();
            n.setDate(date);
            return n;
        });
        note.setContent(content);
        return dayNoteRepository.save(note);
    }
}