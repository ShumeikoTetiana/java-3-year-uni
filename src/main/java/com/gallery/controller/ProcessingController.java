package com.gallery.controller;

import com.gallery.annotation.CurrentUser;
import com.gallery.dto.ProcessingReport;
import com.gallery.service.PhotoProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class ProcessingController {

    private final PhotoProcessingService processingService;

    @PostMapping("/{id}/process")
    public ProcessingReport process(@PathVariable Long id, @CurrentUser String username) {
        return processingService.process(id, username);
    }
}
