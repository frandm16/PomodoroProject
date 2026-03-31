package com.frandm.studytracker.backend.service;

import com.frandm.studytracker.backend.model.Tag;
import com.frandm.studytracker.backend.repository.TagRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<Tag> getAll() {
        return tagRepository.findAllByOrderByNameAsc();
    }

    public List<Tag> getActive() {
        return tagRepository.findByIsArchivedFalseOrderByNameAsc();
    }

    public List<Tag> getFavorites() {
        return tagRepository.findByIsArchivedFalseAndIsFavoriteTrueOrderByNameAsc();
    }

    public Tag getById(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tag not found: " + id));
    }

    public Tag getOrCreate(String name, String color) {
        return tagRepository.findByName(name).orElseGet(() -> {
            Tag tag = new Tag();
            tag.setName(name);
            tag.setColor(color);
            return tagRepository.save(tag);
        });
    }

    public Tag fullUpdate(Long id, String name, String color) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tag not found: " + id));
        tag.setName(name);
        tag.setColor(color);
        return tagRepository.save(tag);
    }

    public Tag partialUpdate(Long id, String name, String color, Boolean isArchived, Boolean isFavorite) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tag not found: " + id));
        if (name != null) tag.setName(name);
        if (color != null) tag.setColor(color);
        if (isArchived != null) tag.setArchived(isArchived);
        if (isFavorite != null) tag.setFavorite(isFavorite);
        return tagRepository.save(tag);
    }

    public void delete(Long id) {
        tagRepository.deleteById(id);
    }
}
