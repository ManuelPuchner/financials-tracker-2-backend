package com.manuelpuchner.backend.transactionrule.dto;

import com.manuelpuchner.backend.transactionrule.entity.RuleTargetField;
import com.manuelpuchner.backend.usercategory.dto.UserCategoryResponse;
import lombok.Builder;

import java.time.Instant;

@Builder
public record TransactionRuleResponse(
        Long id,
        String pattern,
        RuleTargetField targetField,
        UserCategoryResponse userCategory,
        Integer priority,
        Instant createdAt,
        Instant updatedAt
) {}
