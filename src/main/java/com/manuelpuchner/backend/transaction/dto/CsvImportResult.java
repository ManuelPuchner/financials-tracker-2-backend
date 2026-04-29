package com.manuelpuchner.backend.transaction.dto;

import lombok.Builder;

@Builder
public record CsvImportResult(
        int imported,
        int skipped,
        int total
) {}
