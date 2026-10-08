package com.gallery.controller;

import com.gallery.annotation.CurrentUser;
import com.gallery.annotation.PostCreated;
import com.gallery.dto.PhotoRequest;
import com.gallery.dto.PhotoResponse;
import com.gallery.service.PhotoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    @PostCreated
    public PhotoResponse create(@Valid @RequestBody PhotoRequest request, @CurrentUser String username) {
        return photoService.create(request, username);
    }

    @GetMapping
    public List<PhotoResponse> getAll(@CurrentUser String username) {
        return photoService.getAll(username);
    }

    @GetMapping("/{id}")
    public PhotoResponse getById(@PathVariable Long id, @CurrentUser String username) {
        return photoService.getById(id, username);
    }

    @PutMapping("/{id}")
    public PhotoResponse update(@PathVariable Long id, @Valid @RequestBody PhotoRequest request, @CurrentUser String username) {
        return photoService.update(id, request, username);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @CurrentUser String username) {
        photoService.delete(id, username);
    }

    @GetMapping("/admin/all")
    public List<PhotoResponse> getAllForAdmin() {
        return photoService.getAllForAdmin();
    }
}
