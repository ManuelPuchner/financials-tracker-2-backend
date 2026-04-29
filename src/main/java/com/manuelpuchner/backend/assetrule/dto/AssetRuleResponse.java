package com.manuelpuchner.backend.assetrule.dto;

import com.manuelpuchner.backend.asset.entity.AssetClass;
import com.manuelpuchner.backend.assetrule.entity.AssetRuleTargetField;
import com.manuelpuchner.backend.usercategory.dto.UserCategoryResponse;
import lombok.Builder;

import java.time.Instant;

@Builder
public record AssetRuleResponse(
        Long id,
        String pattern,
        AssetRuleTargetField targetField,
        AssetClass assetClass,
        UserCategoryResponse userCategory,
        Integer priority,
        Instant createdAt,
        Instant updatedAt
) {}
