package com.gallery.aspect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuditLog {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    public void record(String user, String action, String method, String outcome) {
        AUDIT.info("user={} action={} method={} outcome={}", user, action, method, outcome);
    }
}
