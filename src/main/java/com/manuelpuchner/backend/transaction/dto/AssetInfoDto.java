package com.manuelpuchner.backend.transaction.dto;

import com.manuelpuchner.backend.asset.entity.AssetClass;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AssetInfoDto(
        Long assetId,
        String symbol,
        String name,
        AssetClass assetClass,
        BigDecimal shares,
        BigDecimal price
) {}
