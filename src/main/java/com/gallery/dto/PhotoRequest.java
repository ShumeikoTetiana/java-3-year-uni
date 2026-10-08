package com.gallery.dto;

import com.gallery.annotation.ValidPhotoAccess;
import com.gallery.model.AccessLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

@ValidPhotoAccess
public record PhotoRequest(
        @NotBlank String title,
        @NotBlank String url,
        String description,
        @NotNull AccessLevel accessLevel,
        Long albumId,
        Set<Long> tagIds) {}
