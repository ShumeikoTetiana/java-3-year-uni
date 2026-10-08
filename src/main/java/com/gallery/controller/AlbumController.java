package com.gallery.controller;

import com.gallery.annotation.CurrentUser;
import com.gallery.annotation.PostCreated;
import com.gallery.dto.AlbumRequest;
import com.gallery.dto.AlbumResponse;
import com.gallery.service.AlbumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/albums")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;

    @PostCreated
    public AlbumResponse create(@Valid @RequestBody AlbumRequest request, @CurrentUser String username) {
        return albumService.create(request, username);
    }

    @GetMapping
    public List<AlbumResponse> getAll(@CurrentUser String username) {
        return albumService.getAll(username);
    }

    @GetMapping("/{id}")
    public AlbumResponse getById(@PathVariable Long id, @CurrentUser String username) {
        return albumService.getById(id, username);
    }

    @PutMapping("/{id}")
    public AlbumResponse update(@PathVariable Long id, @Valid @RequestBody AlbumRequest request, @CurrentUser String username) {
        return albumService.update(id, request, username);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @CurrentUser String username) {
        albumService.delete(id, username);
    }
}
