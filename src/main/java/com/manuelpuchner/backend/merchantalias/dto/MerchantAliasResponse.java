package com.manuelpuchner.backend.merchantalias.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record MerchantAliasResponse(
        Long id,
        String pattern,
        String canonicalName,
        Instant createdAt,
        Instant updatedAt
) {}
