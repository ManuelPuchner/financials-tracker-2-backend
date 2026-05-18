package com.manuelpuchner.backend.asset.service;

import com.manuelpuchner.backend.asset.dto.AssetResponse;
import com.manuelpuchner.backend.asset.dto.AssetSummaryProjection;
import com.manuelpuchner.backend.asset.entity.Asset;
import com.manuelpuchner.backend.asset.repository.AssetRepository;
import com.manuelpuchner.backend.common.dto.EntityStatsDto;
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
public class AssetService {

    private final AssetRepository assetRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Transactional(readOnly = true)
    public Page<AssetResponse> findAll(String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String query = (q != null && !q.isBlank()) ? q.trim() : null;
        Page<AssetSummaryProjection> projections = query != null
                ? transactionRepository.searchAssetStats(query, pageable)
                : transactionRepository.findAllAssetStats(pageable);
        return projections.map(p -> AssetResponse.builder()
                .id(p.getId())
                .symbol(p.getSymbol())
                .name(p.getName())
                .assetClass(p.getAssetClass())
                .stats(EntityStatsDto.builder()
                        .totalIncome(p.getTotalIncome())
                        .totalOutgoing(p.getTotalOutgoing())
                        .net(p.getNet())
                        .transactionCount(p.getTransactionCount())
                        .build())
                .build());
    }

    @Transactional(readOnly = true)
    public AssetResponse findById(Long id, Long accountId) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Asset not found: " + id));
        EntityStatsDto stats = transactionRepository.statsForAsset(id, accountId);
        return AssetResponse.builder()
                .id(asset.getId())
                .symbol(asset.getSymbol())
                .name(asset.getName())
                .assetClass(asset.getAssetClass())
                .stats(stats)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> findTransactions(Long id, Long accountId, int page, int size) {
        if (!assetRepository.existsById(id)) {
            throw new EntityNotFoundException("Asset not found: " + id);
        }
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findByAssetId(id, accountId, pageable)
                .map(transactionMapper::toResponse);
    }
}
