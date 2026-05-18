package com.manuelpuchner.backend.merchant.dto;

import com.manuelpuchner.backend.common.dto.EntityStatsDto;
import lombok.Builder;

@Builder
public record MerchantResponse(
        String name,
        EntityStatsDto stats
) {}
