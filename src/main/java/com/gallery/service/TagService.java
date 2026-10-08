package com.gallery.service;

import com.gallery.dto.TagDto;
import com.gallery.exception.NotFoundException;
import com.gallery.mapper.TagMapper;
import com.gallery.model.Tag;
import com.gallery.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    public TagDto create(TagDto dto) {
        if (tagRepository.existsByName(dto.name())) {
            throw new IllegalArgumentException("Tag already exists: " + dto.name());
        }
        return tagMapper.toDto(tagRepository.save(tagMapper.toEntity(dto)));
    }

    @Transactional(readOnly = true)
    public List<TagDto> getAll() {
        return tagRepository.findAll().stream().map(tagMapper::toDto).toList();
    }

    public TagDto update(Long id, TagDto dto) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tag not found: " + id));
        tagMapper.update(dto, tag);
        return tagMapper.toDto(tagRepository.save(tag));
    }

    public void delete(Long id) {
        if (!tagRepository.existsById(id)) {
            throw new NotFoundException("Tag not found: " + id);
        }
        tagRepository.deleteById(id);
    }
}
