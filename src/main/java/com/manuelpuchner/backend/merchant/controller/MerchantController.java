package com.manuelpuchner.backend.merchant.controller;

import com.manuelpuchner.backend.merchant.dto.MerchantResponse;
import com.manuelpuchner.backend.merchant.service.MerchantService;
import com.manuelpuchner.backend.transaction.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @GetMapping
    public Page<MerchantResponse> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return merchantService.findAll(q, page, size);
    }

    @GetMapping("/{name}")
    public MerchantResponse getByName(
            @PathVariable String name,
            @RequestParam(required = false) Long accountId) {
        return merchantService.findByName(name, accountId);
    }

    @GetMapping("/{name}/transactions")
    public Page<TransactionResponse> getTransactions(
            @PathVariable String name,
            @RequestParam(required = false) Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return merchantService.findTransactions(name, accountId, page, size);
    }
}
