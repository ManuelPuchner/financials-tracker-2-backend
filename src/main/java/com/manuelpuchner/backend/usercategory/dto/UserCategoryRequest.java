package com.manuelpuchner.backend.usercategory.dto;

import jakarta.validation.constraints.NotBlank;

public record UserCategoryRequest(
        @NotBlank String name,
        String color,
        String icon
) {}
