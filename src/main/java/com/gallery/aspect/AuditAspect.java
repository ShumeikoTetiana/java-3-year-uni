package com.gallery.aspect;

import com.gallery.annotation.AuditTrail;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLog auditLog;

    @AfterReturning("@annotation(auditTrail)")
    public void recordSuccess(JoinPoint joinPoint, AuditTrail auditTrail) {
        auditLog.record(UserContext.currentUser(), auditTrail.value(),
                joinPoint.getSignature().toShortString(), "SUCCESS");
    }

    @AfterThrowing(pointcut = "@annotation(auditTrail)", throwing = "ex")
    public void recordFailure(JoinPoint joinPoint, AuditTrail auditTrail, Throwable ex) {
        auditLog.record(UserContext.currentUser(), auditTrail.value(),
                joinPoint.getSignature().toShortString(),
                "FAILED: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
    }
}
