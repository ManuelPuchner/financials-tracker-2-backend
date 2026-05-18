package com.manuelpuchner.backend.counterparty.dto;

import com.manuelpuchner.backend.common.dto.EntityStatsDto;
import lombok.Builder;

@Builder
public record CounterpartyResponse(
        Long id,
        String iban,
        String name,
        EntityStatsDto stats
) {}
