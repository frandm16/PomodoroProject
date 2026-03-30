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

    public Tag getOrCreate(String name, String color) {
        return tagRepository.findByName(name).orElseGet(() -> {
            Tag tag = new Tag();
            tag.setName(name);
            tag.setColor(color);
            return tagRepository.save(tag);
        });
    }

    public Tag update(Long id, String color) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tag not found"));
        if (color != null) tag.setColor(color);
        return tagRepository.save(tag);
    }

    public void delete(String name) {
        tagRepository.findByName(name).ifPresent(tagRepository::delete);
    }
}
