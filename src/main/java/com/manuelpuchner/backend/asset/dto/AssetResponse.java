package com.manuelpuchner.backend.asset.dto;

import com.manuelpuchner.backend.asset.entity.AssetClass;
import com.manuelpuchner.backend.common.dto.EntityStatsDto;
import lombok.Builder;

@Builder
public record AssetResponse(
        Long id,
        String symbol,
        String name,
        AssetClass assetClass,
        EntityStatsDto stats
) {}
