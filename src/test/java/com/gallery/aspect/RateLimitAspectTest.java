package com.gallery.aspect;

import com.gallery.annotation.RateLimited;
import com.gallery.exception.TooManyRequestsException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    static class Target {
        @RateLimited(maxCalls = 2, perSeconds = 60)
        void limited() {
        }
    }

    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private Signature signature;

    private MutableClock clock;
    private RateLimitAspect aspect;
    private RateLimited annotation;

    @BeforeEach
    void setUp() throws Exception {
        clock = new MutableClock();
        aspect = new RateLimitAspect(clock);
        annotation = Target.class.getDeclaredMethod("limited").getAnnotation(RateLimited.class);
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.toShortString()).thenReturn("PhotoService.create(..)");
        loginAs("alice");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String username) {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(username, "pw", "ROLE_USER"));
    }

    @Test
    void limit_callsWithinLimit_proceedsEveryTimeAndReturnsItsResult() throws Throwable {
        when(joinPoint.proceed()).thenReturn("result");

        Object first = aspect.limit(joinPoint, annotation);
        Object second = aspect.limit(joinPoint, annotation);

        assertEquals("result", first);
        assertEquals("result", second);
        verify(joinPoint, times(2)).proceed();
    }

    @Test
    void limit_callOverLimit_throwsTooManyRequestsAndDoesNotProceed() throws Throwable {
        aspect.limit(joinPoint, annotation);
        aspect.limit(joinPoint, annotation);

        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> aspect.limit(joinPoint, annotation));

        assertTrue(ex.getMessage().contains("2 calls per 60 seconds"));
        verify(joinPoint, times(2)).proceed();
    }

    @Test
    void limit_afterWindowPasses_allowsCallsAgain() throws Throwable {
        aspect.limit(joinPoint, annotation);
        aspect.limit(joinPoint, annotation);
        clock.advance(Duration.ofSeconds(61));

        aspect.limit(joinPoint, annotation);

        verify(joinPoint, times(3)).proceed();
    }

    @Test
    void limit_stillInsideWindow_keepsBlocking() throws Throwable {
        aspect.limit(joinPoint, annotation);
        aspect.limit(joinPoint, annotation);
        clock.advance(Duration.ofSeconds(30));

        assertThrows(TooManyRequestsException.class, () -> aspect.limit(joinPoint, annotation));
        verify(joinPoint, times(2)).proceed();
    }

    @Test
    void limit_differentUsers_haveSeparateLimits() throws Throwable {
        aspect.limit(joinPoint, annotation);
        aspect.limit(joinPoint, annotation);
        loginAs("bob");

        aspect.limit(joinPoint, annotation);

        verify(joinPoint, times(3)).proceed();
    }

    @Test
    void limit_methodThrows_exceptionIsPropagated() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class, () -> aspect.limit(joinPoint, annotation));
    }
}
