package com.manuelpuchner.backend.counterparty.controller;

import com.manuelpuchner.backend.counterparty.dto.CounterpartyResponse;
import com.manuelpuchner.backend.counterparty.service.CounterpartyService;
import com.manuelpuchner.backend.transaction.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/counterparties")
@RequiredArgsConstructor
public class CounterpartyController {

    private final CounterpartyService counterpartyService;

    @GetMapping
    public Page<CounterpartyResponse> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String ibanPrefix,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return counterpartyService.findAll(q, ibanPrefix, page, size);
    }

    @GetMapping("/{id}")
    public CounterpartyResponse getById(
            @PathVariable Long id,
            @RequestParam(required = false) Long accountId) {
        return counterpartyService.findById(id, accountId);
    }

    @GetMapping("/{id}/transactions")
    public Page<TransactionResponse> getTransactions(
            @PathVariable Long id,
            @RequestParam(required = false) Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return counterpartyService.findTransactions(id, accountId, page, size);
    }
}
