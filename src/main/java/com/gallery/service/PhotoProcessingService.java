package com.gallery.service;

import com.gallery.dto.ProcessingReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 *  оркестрація. Запускає мініатюру і скан ПАРАЛЕЛЬНО, об'єднує результати.
 */
@Service
@RequiredArgsConstructor
public class PhotoProcessingService {

    private final PhotoService photoService;
    private final MediaService mediaService;

    public ProcessingReport process(Long photoId, String owner) {
        photoService.getById(photoId, owner);

        long start = System.currentTimeMillis();
        CompletableFuture<String> thumbnail = mediaService.generateThumbnail(photoId)
                .exceptionally(ex -> "thumbnail unavailable");
        CompletableFuture<String> scan = mediaService.scanPhoto(photoId)
                .exceptionally(ex -> "UNKNOWN");

        return thumbnail.thenCombine(scan, (t, s) ->
                        new ProcessingReport(photoId, t, s, System.currentTimeMillis() - start))
                .orTimeout(5, TimeUnit.SECONDS)
                .join();
    }
}
