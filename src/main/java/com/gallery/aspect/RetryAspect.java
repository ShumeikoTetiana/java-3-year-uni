package com.gallery.aspect;

import com.gallery.annotation.Retry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Aspect
@Component
@Order(3)
public class RetryAspect {

    private static final Logger log = LoggerFactory.getLogger(RetryAspect.class);

    @Around("@annotation(retry)")
    public Object retry(ProceedingJoinPoint joinPoint, Retry retry) throws Throwable {
        int attempts = Math.max(1, retry.maxAttempts());
        Throwable lastError = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return joinPoint.proceed();
            } catch (Throwable ex) {
                if (!isRetryable(ex, retry)) {
                    throw ex;
                }
                lastError = ex;
                log.warn("Attempt {}/{} of {} failed: {}", attempt, attempts,
                        joinPoint.getSignature().toShortString(), ex.toString());
                if (attempt < attempts) {
                    pause(retry.delayMillis());
                }
            }
        }
        throw lastError;
    }

    private boolean isRetryable(Throwable ex, Retry retry) {
        for (Class<? extends Throwable> type : retry.retryFor()) {
            if (type.isInstance(ex)) {
                return true;
            }
        }
        return false;
    }

    private void pause(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
