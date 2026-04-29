package com.manuelpuchner.backend.transactionrule.controller;

import com.manuelpuchner.backend.transactionrule.dto.TransactionRuleRequest;
import com.manuelpuchner.backend.transactionrule.dto.TransactionRuleResponse;
import com.manuelpuchner.backend.transactionrule.service.TransactionRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transaction-rules")
@RequiredArgsConstructor
public class TransactionRuleController {

    private final TransactionRuleService service;

    @GetMapping
    public List<TransactionRuleResponse> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public TransactionRuleResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<TransactionRuleResponse> create(@RequestBody @Valid TransactionRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public TransactionRuleResponse update(@PathVariable Long id, @RequestBody @Valid TransactionRuleRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
