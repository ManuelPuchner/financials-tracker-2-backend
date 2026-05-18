package com.manuelpuchner.backend.counterpartymerchant.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record CounterpartyMerchantMappingResponse(
        Long id,
        Long counterpartyId,
        String counterpartyIban,
        String counterpartyName,
        String merchantName,
        Instant createdAt,
        Instant updatedAt
) {}
