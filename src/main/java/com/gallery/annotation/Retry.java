package com.gallery.annotation;

import org.springframework.dao.TransientDataAccessException;

import java.lang.annotation.*;


@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Retry {

    int maxAttempts() default 3;

    Class<? extends Throwable>[] retryFor() default {TransientDataAccessException.class};

    long delayMillis() default 0;
}
