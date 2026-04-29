package com.manuelpuchner.backend.transactionrule.dto;

import com.manuelpuchner.backend.transactionrule.entity.RuleTargetField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransactionRuleRequest(
        @NotBlank String pattern,
        @NotNull RuleTargetField targetField,
        @NotNull Long userCategoryId,
        Integer priority
) {}
