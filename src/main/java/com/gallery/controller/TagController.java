package com.gallery.controller;

import com.gallery.annotation.PostCreated;
import com.gallery.dto.TagDto;
import com.gallery.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @PostCreated
    public TagDto create(@Valid @RequestBody TagDto dto) {
        return tagService.create(dto);
    }

    @GetMapping
    public List<TagDto> getAll() {
        return tagService.getAll();
    }

    @PutMapping("/{id}")
    public TagDto update(@PathVariable Long id, @Valid @RequestBody TagDto dto) {
        return tagService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        tagService.delete(id);
    }
}
