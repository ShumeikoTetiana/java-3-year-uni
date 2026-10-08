package com.gallery.controller;

import com.gallery.dto.AlbumRequest;
import com.gallery.dto.AlbumResponse;
import com.gallery.service.AlbumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/albums")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlbumResponse create(@Valid @RequestBody AlbumRequest request, Principal principal) {
        return albumService.create(request, principal.getName());
    }

    @GetMapping
    public List<AlbumResponse> getAll(Principal principal) {
        return albumService.getAll(principal.getName());
    }

    @GetMapping("/{id}")
    public AlbumResponse getById(@PathVariable Long id, Principal principal) {
        return albumService.getById(id, principal.getName());
    }

    @PutMapping("/{id}")
    public AlbumResponse update(@PathVariable Long id, @Valid @RequestBody AlbumRequest request, Principal principal) {
        return albumService.update(id, request, principal.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Principal principal) {
        albumService.delete(id, principal.getName());
    }
}
