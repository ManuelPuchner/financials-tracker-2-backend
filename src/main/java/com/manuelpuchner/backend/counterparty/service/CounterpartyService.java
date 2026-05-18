package com.manuelpuchner.backend.counterparty.service;

import com.manuelpuchner.backend.common.dto.EntityStatsDto;
import com.manuelpuchner.backend.counterparty.dto.CounterpartyResponse;
import com.manuelpuchner.backend.counterparty.dto.CounterpartySummaryProjection;
import com.manuelpuchner.backend.counterparty.entity.Counterparty;
import com.manuelpuchner.backend.counterparty.repository.CounterpartyRepository;
import com.manuelpuchner.backend.transaction.dto.TransactionResponse;
import com.manuelpuchner.backend.transaction.repository.TransactionRepository;
import com.manuelpuchner.backend.transaction.service.TransactionMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CounterpartyService {

    private final CounterpartyRepository counterpartyRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Transactional(readOnly = true)
    public Page<CounterpartyResponse> findAll(String q, String ibanPrefix, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String query = (q != null && !q.isBlank()) ? q.trim() : null;
        String prefixPattern = (ibanPrefix != null && !ibanPrefix.isBlank()) ? ibanPrefix.trim() + "%" : null;
        Page<CounterpartySummaryProjection> projections = query != null
                ? transactionRepository.searchCounterpartyStats(query, prefixPattern, pageable)
                : transactionRepository.findAllCounterpartyStats(prefixPattern, pageable);
        return projections.map(p -> CounterpartyResponse.builder()
                .id(p.getId())
                .iban(p.getIban())
                .name(p.getName())
                .stats(EntityStatsDto.builder()
                        .totalIncome(p.getTotalIncome())
                        .totalOutgoing(p.getTotalOutgoing())
                        .net(p.getNet())
                        .transactionCount(p.getTransactionCount())
                        .build())
                .build());
    }

    @Transactional(readOnly = true)
    public CounterpartyResponse findById(Long id, Long accountId) {
        Counterparty cp = counterpartyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Counterparty not found: " + id));
        EntityStatsDto stats = transactionRepository.statsForCounterparty(id, accountId);
        return CounterpartyResponse.builder()
                .id(cp.getId())
                .iban(cp.getIban())
                .name(cp.getName())
                .stats(stats)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> findTransactions(Long id, Long accountId, int page, int size) {
        if (!counterpartyRepository.existsById(id)) {
            throw new EntityNotFoundException("Counterparty not found: " + id);
        }
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findByCounterpartyId(id, accountId, pageable)
                .map(transactionMapper::toResponse);
    }
}
