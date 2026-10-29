package com.gallery.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**  дві незалежні повільні операції з результатом (CompletableFuture). */
@Slf4j
@Service
public class MediaService {

    @Async
    public CompletableFuture<String> generateThumbnail(Long photoId) {
        log.info("[{}] Generating thumbnail for photo {}", Thread.currentThread().getName(), photoId);
        sleep(1000);
        return CompletableFuture.completedFuture("thumb_" + photoId + ".jpg");
    }

    @Async
    public CompletableFuture<String> scanPhoto(Long photoId) {
        log.info("[{}] Scanning photo {}", Thread.currentThread().getName(), photoId);
        sleep(1000);
        return CompletableFuture.completedFuture("CLEAN");
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
