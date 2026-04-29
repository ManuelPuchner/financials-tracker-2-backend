package com.manuelpuchner.backend.sparkasserule.dto;

import com.manuelpuchner.backend.sparkasserule.entity.RuleTargetField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SparkasseRuleRequest(
        @NotBlank String pattern,
        @NotNull RuleTargetField targetField,
        @NotNull Long userCategoryId,
        Integer priority
) {}
