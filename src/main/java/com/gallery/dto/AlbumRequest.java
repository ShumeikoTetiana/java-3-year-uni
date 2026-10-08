package com.gallery.dto;

import jakarta.validation.constraints.NotBlank;

public record AlbumRequest(@NotBlank String name, String description) {}
