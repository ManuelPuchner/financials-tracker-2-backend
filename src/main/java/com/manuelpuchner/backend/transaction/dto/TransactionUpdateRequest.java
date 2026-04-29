package com.manuelpuchner.backend.transaction.dto;

import com.manuelpuchner.backend.transaction.entity.Category;

public record TransactionUpdateRequest(
        String note,
        String description,
        String merchantName,
        String mccCode,
        Long userCategoryId,
        Category category
) {}
