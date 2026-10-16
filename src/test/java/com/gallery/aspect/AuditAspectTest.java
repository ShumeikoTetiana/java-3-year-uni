package com.gallery.aspect;

import com.gallery.annotation.AuditTrail;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    static class Target {
        @AuditTrail("photo.create")
        void audited() {
        }
    }

    @Mock
    private JoinPoint joinPoint;
    @Mock
    private Signature signature;
    @Mock
    private AuditLog auditLog;

    private AuditAspect aspect;
    private AuditTrail annotation;

    @BeforeEach
    void setUp() throws Exception {
        aspect = new AuditAspect(auditLog);
        annotation = Target.class.getDeclaredMethod("audited").getAnnotation(AuditTrail.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("PhotoService.create(..)");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordSuccess_authenticatedUser_writesSuccessEntry() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("alice", "pw", "ROLE_USER"));

        aspect.recordSuccess(joinPoint, annotation);

        verify(auditLog).record("alice", "photo.create", "PhotoService.create(..)", "SUCCESS");
    }

    @Test
    void recordSuccess_noAuthentication_writesAnonymousUser() {
        aspect.recordSuccess(joinPoint, annotation);

        verify(auditLog).record("anonymous", "photo.create", "PhotoService.create(..)", "SUCCESS");
    }

    @Test
    void recordFailure_methodThrew_writesFailedEntryWithReason() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("bob", "pw", "ROLE_USER"));

        aspect.recordFailure(joinPoint, annotation, new IllegalStateException("db down"));

        verify(auditLog).record("bob", "photo.create", "PhotoService.create(..)",
                "FAILED: IllegalStateException: db down");
        verify(auditLog, never()).record(anyString(), anyString(), anyString(), eq("SUCCESS"));
    }
}
