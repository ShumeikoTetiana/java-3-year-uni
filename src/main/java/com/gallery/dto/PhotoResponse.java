package com.gallery.dto;

import com.gallery.model.AccessLevel;

import java.time.LocalDateTime;
import java.util.Set;

public record PhotoResponse(
        Long id,
        String title,
        String url,
        String description,
        AccessLevel accessLevel,
        Long albumId,
        Set<TagDto> tags,
        LocalDateTime createdAt) {}
