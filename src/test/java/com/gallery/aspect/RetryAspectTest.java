package com.gallery.aspect;

import com.gallery.annotation.Retry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryAspectTest {

    static class Target {
        @Retry(maxAttempts = 3)
        void retried() {
        }
    }

    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private Signature signature;

    private final RetryAspect aspect = new RetryAspect();
    private Retry annotation;

    @BeforeEach
    void setUp() throws Exception {
        annotation = Target.class.getDeclaredMethod("retried").getAnnotation(Retry.class);
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.toShortString()).thenReturn("TagService.create(..)");
    }

    @Test
    void retry_firstAttemptSucceeds_proceedsOnceAndReturnsResult() throws Throwable {
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.retry(joinPoint, annotation);

        assertEquals("ok", result);
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void retry_transientFailureThenSuccess_retriesAndReturnsResult() throws Throwable {
        when(joinPoint.proceed())
                .thenThrow(new QueryTimeoutException("timeout"))
                .thenReturn("ok");

        Object result = aspect.retry(joinPoint, annotation);

        assertEquals("ok", result);
        verify(joinPoint, times(2)).proceed();
    }

    @Test
    void retry_allAttemptsFail_throwsLastErrorAfterMaxAttempts() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new QueryTimeoutException("timeout"));

        QueryTimeoutException ex = assertThrows(QueryTimeoutException.class,
                () -> aspect.retry(joinPoint, annotation));

        assertEquals("timeout", ex.getMessage());
        verify(joinPoint, times(3)).proceed();
    }

    @Test
    void retry_nonRetryableException_isRethrownImmediatelyWithoutRetry() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new IllegalArgumentException("bad input"));

        assertThrows(IllegalArgumentException.class, () -> aspect.retry(joinPoint, annotation));
        verify(joinPoint, times(1)).proceed();
    }
}
