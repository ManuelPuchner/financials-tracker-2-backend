package com.manuelpuchner.backend.transaction.dto;

import com.manuelpuchner.backend.usercategory.dto.UserCategoryResponse;
import lombok.Builder;

@Builder
public record MccCodeDto(
        String mcc,
        String description,
        UserCategoryResponse userCategory
) {}
