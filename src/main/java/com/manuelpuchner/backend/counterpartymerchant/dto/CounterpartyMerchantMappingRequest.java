package com.manuelpuchner.backend.counterpartymerchant.dto;

public record CounterpartyMerchantMappingRequest(
        Long counterpartyId,
        String merchantName
) {}
