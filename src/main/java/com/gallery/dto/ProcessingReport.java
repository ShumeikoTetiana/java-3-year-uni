package com.gallery.dto;

public record ProcessingReport(Long photoId, String thumbnail, String scanResult, long tookMs) {
}
