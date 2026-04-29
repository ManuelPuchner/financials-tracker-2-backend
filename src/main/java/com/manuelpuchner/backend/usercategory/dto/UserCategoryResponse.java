package com.manuelpuchner.backend.usercategory.dto;

import lombok.Builder;

@Builder
public record UserCategoryResponse(
        Long id,
        String name,
        String color,
        String icon
) {}
