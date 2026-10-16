package com.gallery.aspect;

import com.gallery.annotation.RateLimited;
import com.gallery.exception.TooManyRequestsException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Aspect
@Component
@Order(2)
public class RateLimitAspect {

    private final Clock clock;
    private final Map<String, Deque<Instant>> calls = new ConcurrentHashMap<>();

    public RateLimitAspect() {
        this(Clock.systemUTC());
    }

    RateLimitAspect(Clock clock) {
        this.clock = clock;
    }

    @Around("@annotation(rateLimited)")
    public Object limit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        String key = UserContext.currentUser() + "|" + joinPoint.getSignature().toShortString();
        Instant now = clock.instant();
        Instant windowStart = now.minusSeconds(rateLimited.perSeconds());

        Deque<Instant> window = calls.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && !window.peekFirst().isAfter(windowStart)) {
                window.pollFirst();
            }
            if (window.size() >= rateLimited.maxCalls()) {
                throw new TooManyRequestsException("Rate limit exceeded: at most "
                        + rateLimited.maxCalls() + " calls per " + rateLimited.perSeconds() + " seconds");
            }
            window.addLast(now);
        }
        return joinPoint.proceed();
    }
}
