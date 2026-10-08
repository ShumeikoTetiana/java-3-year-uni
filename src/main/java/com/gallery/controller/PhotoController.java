package com.gallery.controller;

import com.gallery.dto.PhotoRequest;
import com.gallery.dto.PhotoResponse;
import com.gallery.service.PhotoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PhotoResponse create(@Valid @RequestBody PhotoRequest request, Principal principal) {
        return photoService.create(request, principal.getName());
    }

    @GetMapping
    public List<PhotoResponse> getAll(Principal principal) {
        return photoService.getAll(principal.getName());
    }

    @GetMapping("/{id}")
    public PhotoResponse getById(@PathVariable Long id, Principal principal) {
        return photoService.getById(id, principal.getName());
    }

    @PutMapping("/{id}")
    public PhotoResponse update(@PathVariable Long id, @Valid @RequestBody PhotoRequest request, Principal principal) {
        return photoService.update(id, request, principal.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Principal principal) {
        photoService.delete(id, principal.getName());
    }

    /** Фільтр-ланцюг пускає будь-якого автентифікованого, а ADMIN перевіряє @PreAuthorize у сервісі. */
    @GetMapping("/admin/all")
    public List<PhotoResponse> getAllForAdmin() {
        return photoService.getAllForAdmin();
    }
}
