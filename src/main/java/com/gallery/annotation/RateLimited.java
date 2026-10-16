package com.gallery.annotation;

import java.lang.annotation.*;


@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

    int maxCalls() default 10;

    int perSeconds() default 60;
}
