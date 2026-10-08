package com.gallery.dto;

import jakarta.validation.constraints.NotBlank;

public record TagDto(Long id, @NotBlank String name) {}
