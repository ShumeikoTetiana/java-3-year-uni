package com.gallery.exception;

/** Перевищено ліміт викликів (HTTP 429). Кидається аспектом RateLimitAspect. */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
