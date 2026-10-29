package com.gallery.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** "fire-and-forget" — імітація повільної відправки листа після завантаження фото. */
@Slf4j
@Service
public class NotificationService {

    @Async
    public void notifyPhotoUploaded(String owner, String title) {
        log.info("[{}] Sending e-mail to {} about '{}'...", Thread.currentThread().getName(), owner, title);
        sleep(2000);
        log.info("[{}] E-mail to {} sent", Thread.currentThread().getName(), owner);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
