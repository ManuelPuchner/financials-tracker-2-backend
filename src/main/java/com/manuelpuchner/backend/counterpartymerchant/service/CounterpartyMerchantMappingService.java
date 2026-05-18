package com.manuelpuchner.backend.counterpartymerchant.service;

import com.manuelpuchner.backend.counterparty.entity.Counterparty;
import com.manuelpuchner.backend.counterparty.repository.CounterpartyRepository;
import com.manuelpuchner.backend.counterpartymerchant.dto.CounterpartyMerchantMappingRequest;
import com.manuelpuchner.backend.counterpartymerchant.dto.CounterpartyMerchantMappingResponse;
import com.manuelpuchner.backend.counterpartymerchant.entity.CounterpartyMerchantMapping;
import com.manuelpuchner.backend.counterpartymerchant.repository.CounterpartyMerchantMappingRepository;
import com.manuelpuchner.backend.transaction.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CounterpartyMerchantMappingService {

    private final CounterpartyMerchantMappingRepository mappingRepository;
    private final CounterpartyRepository counterpartyRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<CounterpartyMerchantMappingResponse> findAll() {
        return mappingRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<CounterpartyMerchantMappingResponse> findByCounterpartyId(Long counterpartyId) {
        return mappingRepository.findByCounterparty_Id(counterpartyId).map(this::toResponse);
    }

    @Transactional
    public CounterpartyMerchantMappingResponse upsert(CounterpartyMerchantMappingRequest req) {
        Counterparty counterparty = counterpartyRepository.findById(req.counterpartyId())
                .orElseThrow(() -> new EntityNotFoundException("Counterparty not found: " + req.counterpartyId()));

        CounterpartyMerchantMapping mapping = mappingRepository.findByCounterparty_Id(req.counterpartyId())
                .orElseGet(() -> CounterpartyMerchantMapping.builder().counterparty(counterparty).build());

        String oldMerchantName = mapping.getMerchantName();
        mapping.setMerchantName(req.merchantName());
        CounterpartyMerchantMapping saved = mappingRepository.save(mapping);

        // Retroactively set merchant_name on all transactions for this counterparty
        int updated = transactionRepository.bulkSetMerchantNameByCounterparty(counterparty.getId(), req.merchantName());
        log.info("[Retroactive] CounterpartyMerchantMapping counterpartyId={} merchant='{}' updated={}", counterparty.getId(), req.merchantName(), updated);

        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        CounterpartyMerchantMapping mapping = mappingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Mapping not found: " + id));

        // Only clear merchant_name for rows that were set by this mapping (raw_merchant_name is null = not a real card merchant)
        int cleared = transactionRepository.bulkClearMerchantNameByCounterparty(
                mapping.getCounterparty().getId(), mapping.getMerchantName());
        log.info("[Retroactive] CounterpartyMerchantMapping delete id={} cleared={}", id, cleared);

        mappingRepository.delete(mapping);
    }

    /** Called at import time to resolve merchant name for a counterparty. */
    public Optional<String> resolveMerchantName(Long counterpartyId) {
        return mappingRepository.findByCounterparty_Id(counterpartyId)
                .map(CounterpartyMerchantMapping::getMerchantName);
    }

    private CounterpartyMerchantMappingResponse toResponse(CounterpartyMerchantMapping m) {
        return CounterpartyMerchantMappingResponse.builder()
                .id(m.getId())
                .counterpartyId(m.getCounterparty().getId())
                .counterpartyIban(m.getCounterparty().getIban())
                .counterpartyName(m.getCounterparty().getName())
                .merchantName(m.getMerchantName())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}
