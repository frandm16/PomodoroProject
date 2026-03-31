package com.frandm.studytracker.backend.service;

import com.frandm.studytracker.backend.model.DayNote;
import com.frandm.studytracker.backend.repository.DayNoteRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class DayNoteService {

    private final DayNoteRepository dayNoteRepository;

    public DayNoteService(DayNoteRepository dayNoteRepository) {
        this.dayNoteRepository = dayNoteRepository;
    }

    public List<DayNote> getAll() {
        return dayNoteRepository.findAll();
    }

    public DayNote getById(Long id) {
        return dayNoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("DayNote not found: " + id));
    }

    public DayNote getOrEmpty(LocalDate date) {
        return dayNoteRepository.findByDate(date).orElseGet(() -> {
            DayNote empty = new DayNote();
            empty.setDate(date);
            empty.setContent("");
            return empty;
        });
    }

    public DayNote create(LocalDate date, String content) {
        return dayNoteRepository.findByDate(date).orElseGet(() -> {
            DayNote note = new DayNote();
            note.setDate(date);
            note.setContent(content != null ? content : "");
            return dayNoteRepository.save(note);
        });
    }

    public DayNote fullUpdate(Long id, LocalDate date, String content) {
        DayNote note = dayNoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("DayNote not found: " + id));
        if (date != null) note.setDate(date);
        note.setContent(content != null ? content : "");
        return dayNoteRepository.save(note);
    }

    public DayNote partialUpdate(Long id, String content) {
        DayNote note = dayNoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("DayNote not found: " + id));
        if (content != null) note.setContent(content);
        return dayNoteRepository.save(note);
    }

    public void delete(Long id) {
        dayNoteRepository.deleteById(id);
    }
}
