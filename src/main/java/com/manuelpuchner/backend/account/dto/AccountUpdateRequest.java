package com.manuelpuchner.backend.account.dto;

public record AccountUpdateRequest(
        String name,
        String color,
        String icon
) {}
