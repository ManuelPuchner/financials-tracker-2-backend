package com.manuelpuchner.backend.merchant.service;

import com.manuelpuchner.backend.common.dto.EntityStatsDto;
import com.manuelpuchner.backend.merchant.dto.MerchantResponse;
import com.manuelpuchner.backend.merchant.dto.MerchantSummaryProjection;
import com.manuelpuchner.backend.transaction.dto.TransactionResponse;
import com.manuelpuchner.backend.transaction.repository.TransactionRepository;
import com.manuelpuchner.backend.transaction.service.TransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Transactional(readOnly = true)
    public Page<MerchantResponse> findAll(String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String query = (q != null && !q.isBlank()) ? q.trim() : null;
        Page<MerchantSummaryProjection> projections = query != null
                ? transactionRepository.searchMerchantStats(query, pageable)
                : transactionRepository.findAllMerchantStats(pageable);
        return projections.map(p -> MerchantResponse.builder()
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
    public MerchantResponse findByName(String name, Long accountId) {
        EntityStatsDto stats = transactionRepository.statsForMerchant(name, accountId);
        return MerchantResponse.builder()
                .name(name)
                .stats(stats)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> findTransactions(String name, Long accountId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findByMerchantName(name, accountId, pageable)
                .map(transactionMapper::toResponse);
    }
}
