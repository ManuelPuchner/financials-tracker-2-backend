package com.manuelpuchner.backend.sparkasserule.dto;

import com.manuelpuchner.backend.sparkasserule.entity.RuleTargetField;
import com.manuelpuchner.backend.usercategory.dto.UserCategoryResponse;
import lombok.Builder;

import java.time.Instant;

@Builder
public record SparkasseRuleResponse(
        Long id,
        String pattern,
        RuleTargetField targetField,
        UserCategoryResponse userCategory,
        Integer priority,
        Instant createdAt,
        Instant updatedAt
) {}
