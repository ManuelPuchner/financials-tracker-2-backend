package com.manuelpuchner.backend.transaction.repository;

public record RecategorizationCandidate(
        Long id,
        String description,
        String merchantName,
        String counterpartyName
) {}
