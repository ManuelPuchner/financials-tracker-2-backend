package com.manuelpuchner.backend.assetrule.dto;

import com.manuelpuchner.backend.asset.entity.AssetClass;
import com.manuelpuchner.backend.assetrule.entity.AssetRuleTargetField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AssetRuleRequest(
        @NotBlank String pattern,
        @NotNull AssetRuleTargetField targetField,
        @NotNull AssetClass assetClass,
        @NotNull Long userCategoryId,
        Integer priority
) {}
