package com.manuelpuchner.backend.merchantalias.dto;

import jakarta.validation.constraints.NotBlank;

public record MerchantAliasRequest(
        @NotBlank String pattern,
        @NotBlank String canonicalName
) {}
