package com.gallery.dto;

import java.time.LocalDateTime;

public record AlbumResponse(Long id, String name, String description, LocalDateTime createdAt) {}
