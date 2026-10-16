package com.gallery.aspect;

import com.gallery.annotation.AuditTrail;
import com.gallery.annotation.RateLimited;
import com.gallery.annotation.Retry;
import com.gallery.exception.TooManyRequestsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aop.framework.AopContext;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.dao.QueryTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;


class AspectProxyTest {

    public static class DemoService {

        private int flakyAttempts;

        @AuditTrail("demo.work")
        public String work() {
            return "done";
        }

        public String workViaThis() {
            return this.work();
        }

        public String workViaCurrentProxy() {
            return ((DemoService) AopContext.currentProxy()).work();
        }

        @RateLimited(maxCalls = 2, perSeconds = 60)
        public String limited() {
            return "ok";
        }

        @Retry(maxAttempts = 3)
        public String flaky() {
            flakyAttempts++;
            if (flakyAttempts < 3) {
                throw new QueryTimeoutException("temporary failure");
            }
            return "ok after " + flakyAttempts;
        }
    }

    @Configuration
    @EnableAspectJAutoProxy(exposeProxy = true)
    static class Config {
        @Bean
        AuditLog auditLog() {
            return Mockito.mock(AuditLog.class);
        }

        @Bean
        AuditAspect auditAspect(AuditLog auditLog) {
            return new AuditAspect(auditLog);
        }

        @Bean
        RateLimitAspect rateLimitAspect() {
            return new RateLimitAspect();
        }

        @Bean
        RetryAspect retryAspect() {
            return new RetryAspect();
        }

        @Bean
        DemoService demoService() {
            return new DemoService();
        }
    }

    private AnnotationConfigApplicationContext context;
    private DemoService demo;
    private AuditLog auditLog;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(Config.class);
        demo = context.getBean(DemoService.class);
        auditLog = context.getBean(AuditLog.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void bean_isWrappedIntoProxy() {
        assertTrue(AopUtils.isAopProxy(demo));
    }

    @Test
    void externalCall_activatesAspect() {
        String result = demo.work();

        assertEquals("done", result);
        verify(auditLog).record(anyString(), eq("demo.work"), anyString(), eq("SUCCESS"));
    }

    @Test
    void selfInvocation_doesNotActivateAspect() {
        String result = demo.workViaThis();

        assertEquals("done", result);
        verifyNoInteractions(auditLog);
    }

    @Test
    void selfInvocationThroughCurrentProxy_activatesAspectAgain() {
        String result = demo.workViaCurrentProxy();

        assertEquals("done", result);
        verify(auditLog).record(anyString(), eq("demo.work"), anyString(), eq("SUCCESS"));
    }

    @Test
    void rateLimited_externalCalls_areBlockedAfterLimit() {
        assertEquals("ok", demo.limited());
        assertEquals("ok", demo.limited());
        assertThrows(TooManyRequestsException.class, () -> demo.limited());
    }

    @Test
    void retry_externalCall_retriesUntilSuccess() {
        String result = demo.flaky();

        assertEquals("ok after 3", result);
    }
}
