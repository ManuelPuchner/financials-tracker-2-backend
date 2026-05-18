package com.manuelpuchner.backend.asset.controller;

import com.manuelpuchner.backend.asset.dto.AssetResponse;
import com.manuelpuchner.backend.asset.service.AssetService;
import com.manuelpuchner.backend.transaction.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @GetMapping
    public Page<AssetResponse> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return assetService.findAll(q, page, size);
    }

    @GetMapping("/{id}")
    public AssetResponse getById(
            @PathVariable Long id,
            @RequestParam(required = false) Long accountId) {
        return assetService.findById(id, accountId);
    }

    @GetMapping("/{id}/transactions")
    public Page<TransactionResponse> getTransactions(
            @PathVariable Long id,
            @RequestParam(required = false) Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return assetService.findTransactions(id, accountId, page, size);
    }
}
