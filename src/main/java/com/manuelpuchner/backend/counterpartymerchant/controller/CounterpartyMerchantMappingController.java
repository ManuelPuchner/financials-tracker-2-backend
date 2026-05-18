package com.manuelpuchner.backend.counterpartymerchant.controller;

import com.manuelpuchner.backend.counterpartymerchant.dto.CounterpartyMerchantMappingRequest;
import com.manuelpuchner.backend.counterpartymerchant.dto.CounterpartyMerchantMappingResponse;
import com.manuelpuchner.backend.counterpartymerchant.service.CounterpartyMerchantMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/counterparty-merchant-mappings")
@RequiredArgsConstructor
public class CounterpartyMerchantMappingController {

    private final CounterpartyMerchantMappingService service;

    @GetMapping
    public List<CounterpartyMerchantMappingResponse> getAll() {
        return service.findAll();
    }

    @GetMapping("/by-counterparty/{counterpartyId}")
    public ResponseEntity<CounterpartyMerchantMappingResponse> getByCounterparty(@PathVariable Long counterpartyId) {
        return service.findByCounterpartyId(counterpartyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public CounterpartyMerchantMappingResponse upsert(@RequestBody CounterpartyMerchantMappingRequest request) {
        return service.upsert(request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
