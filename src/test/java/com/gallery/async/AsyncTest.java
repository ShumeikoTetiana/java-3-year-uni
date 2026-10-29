package com.gallery.async;

import com.gallery.config.AsyncConfig;
import com.gallery.controller.ProcessingController;
import com.gallery.dto.ProcessingReport;
import com.gallery.exception.GlobalExceptionHandler;
import com.gallery.service.MediaService;
import com.gallery.service.NotificationService;
import com.gallery.service.PhotoProcessingService;
import com.gallery.service.PhotoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** перевірка @Async без БД і без Keycloak — лише малий Spring-контекст. */
class AsyncTest {

    static class Probe {
        @Async
        public CompletableFuture<String> threadName() {
            return CompletableFuture.completedFuture(Thread.currentThread().getName());
        }

        @Async
        public void failVoid() {
            throw new IllegalStateException("boom");
        }

        @Async
        public void slow() {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Configuration
    @EnableAsync
    static class TinyConfig {
        @Bean
        ThreadPoolTaskExecutor taskExecutor() {
            ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();
            e.setCorePoolSize(1);
            e.setMaxPoolSize(1);
            e.setQueueCapacity(0);
            e.setThreadNamePrefix("tiny-");
            e.initialize();
            return e;
        }

        @Bean
        Probe probe() {
            return new Probe();
        }
    }

    private AnnotationConfigApplicationContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new AnnotationConfigApplicationContext();
        ctx.register(AsyncConfig.class, NotificationService.class, MediaService.class);
        ctx.registerBean(Probe.class);
        ctx.registerBean(PhotoService.class, () -> mock(PhotoService.class));
        ctx.registerBean(PhotoProcessingService.class);
        ctx.refresh();
    }

    @AfterEach
    void tearDown() {
        ctx.close();
    }

    @Test
    void asyncMethodRunsInGalleryPoolThread() {
        String name = ctx.getBean(Probe.class).threadName().join();
        assertTrue(name.startsWith("gallery-async-"), name);
        assertNotEquals(Thread.currentThread().getName(), name);
    }

    @Test
    void fireAndForgetReturnsImmediately() {
        long start = System.currentTimeMillis();
        ctx.getBean(NotificationService.class).notifyPhotoUploaded("alice", "Sunset"); // ~2с у фоні
        assertTrue(System.currentTimeMillis() - start < 1000, "caller must not wait for e-mail");
    }

    @Test
    void exceptionInVoidAsyncDoesNotReachCaller() {
        assertDoesNotThrow(() -> ctx.getBean(Probe.class).failVoid());
    }

    @Test
    void thumbnailAndScanRunInParallel() {
        ProcessingReport report = ctx.getBean(PhotoProcessingService.class).process(1L, "alice");
        assertEquals("thumb_1.jpg", report.thumbnail());
        assertEquals("CLEAN", report.scanResult());
        assertTrue(report.tookMs() < 1800, "took " + report.tookMs());
    }

    @Test
    void failedSubtaskGivesFallbackInsteadOfError() {
        MediaService media = mock(MediaService.class);
        when(media.generateThumbnail(anyLong())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("x")));
        when(media.scanPhoto(anyLong())).thenReturn(CompletableFuture.completedFuture("CLEAN"));
        ProcessingReport r = new PhotoProcessingService(mock(PhotoService.class), media).process(1L, "alice");
        assertEquals("thumbnail unavailable", r.thumbnail());
        assertEquals("CLEAN", r.scanResult());
    }

    @Test
    void fullPoolRejectsTask() {
        try (AnnotationConfigApplicationContext tiny = new AnnotationConfigApplicationContext(TinyConfig.class)) {
            Probe probe = tiny.getBean(Probe.class);
            probe.slow(); // займає єдиний потік, черги немає
            assertThrows(TaskRejectedException.class, probe::slow);
        }
    }

    @Test
    void rejectedTaskGives503() throws Exception {
        PhotoProcessingService service = mock(PhotoProcessingService.class);
        when(service.process(anyLong(), anyString())).thenThrow(new TaskRejectedException("full"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProcessingController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new com.gallery.resolver.CurrentUserArgumentResolver())
                .build();
        mvc.perform(post("/api/photos/1/process").principal(() -> "alice")).andExpect(status().isServiceUnavailable());
    }

    @Test
    void timeoutOrFailureGives503() throws Exception {
        PhotoProcessingService service = mock(PhotoProcessingService.class);
        when(service.process(anyLong(), anyString())).thenThrow(new CompletionException(new RuntimeException("t")));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProcessingController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new com.gallery.resolver.CurrentUserArgumentResolver())
                .build();
        mvc.perform(post("/api/photos/1/process").principal(() -> "alice")).andExpect(status().isServiceUnavailable());
    }
}
