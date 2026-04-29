package com.manuelpuchner.backend.transaction.dto;

import lombok.Builder;

@Builder
public record CounterpartyInfoDto(
        Long counterpartyId,
        String iban,
        String name,
        String paymentReference
) {}
