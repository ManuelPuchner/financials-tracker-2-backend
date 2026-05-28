package com.manuelpuchner.backend.transaction.repository;

import com.manuelpuchner.backend.asset.dto.AssetSummaryProjection;
import com.manuelpuchner.backend.common.dto.EntityStatsDto;
import com.manuelpuchner.backend.counterparty.dto.CounterpartySummaryProjection;
import com.manuelpuchner.backend.dashboard.dto.AssetPositionDto;
import com.manuelpuchner.backend.dashboard.dto.TopMerchantDto;
import com.manuelpuchner.backend.merchant.dto.MerchantSummaryProjection;
import com.manuelpuchner.backend.transaction.entity.Category;
import com.manuelpuchner.backend.transaction.entity.Transaction;
import com.manuelpuchner.backend.transaction.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByTransactionId(UUID transactionId);

    long countByAccount_Id(Long accountId);

    Optional<Transaction> findByTransactionId(UUID transactionId);

    Page<Transaction> findByCategory(Category category, Pageable pageable);

    Page<Transaction> findByType(TransactionType type, Pageable pageable);

    Page<Transaction> findByDateBetween(LocalDate from, LocalDate to, Pageable pageable);

    Page<Transaction> findByMccCode_Mcc(String mcc, Pageable pageable);

    Page<Transaction> findByUserCategory_Id(Long userCategoryId, Pageable pageable);

    Page<Transaction> findByAccount_Id(Long accountId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.amount IS NOT NULL" +
           " AND (:accountId IS NULL OR t.account.id = :accountId)")
    BigDecimal sumAllAmounts(@Param("accountId") Long accountId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type IN :types AND t.amount IS NOT NULL" +
           " AND (:accountId IS NULL OR t.account.id = :accountId)")
    BigDecimal sumAmountByTypes(@Param("types") Set<TransactionType> types, @Param("accountId") Long accountId);

    @Query("SELECT COALESCE(SUM(COALESCE(t.fee, 0) + COALESCE(t.tax, 0)), 0) FROM Transaction t" +
           " WHERE (:accountId IS NULL OR t.account.id = :accountId)")
    BigDecimal sumFeesAndTaxes(@Param("accountId") Long accountId);

    @Query("""
            SELECT new com.manuelpuchner.backend.dashboard.dto.AssetPositionDto(
                t.asset.symbol,
                t.asset.name,
                SUM(CASE WHEN t.type IN ('BUY', 'SELL', 'STOCKPERK', 'MIGRATION') THEN COALESCE(t.shares, 0) ELSE 0 END),
                COALESCE(-SUM(CASE WHEN t.type IN ('BUY', 'SELL') THEN COALESCE(t.amount, 0) + COALESCE(t.fee, 0) + COALESCE(t.tax, 0) ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN t.type = 'DIVIDEND' THEN t.amount ELSE 0 END), 0)
            )
            FROM Transaction t
            WHERE t.asset IS NOT NULL
              AND (:accountId IS NULL OR t.account.id = :accountId)
            GROUP BY t.asset.symbol, t.asset.name
            HAVING SUM(CASE WHEN t.type IN ('BUY', 'SELL', 'STOCKPERK', 'MIGRATION') THEN COALESCE(t.shares, 0) ELSE 0 END) > 0
            ORDER BY t.asset.symbol
            """)
    List<AssetPositionDto> findAssetPositions(@Param("accountId") Long accountId);

    @Query("""
            SELECT new com.manuelpuchner.backend.dashboard.dto.TopMerchantDto(
                t.merchantName,
                -SUM(t.amount),
                COUNT(t)
            )
            FROM Transaction t
            WHERE t.merchantName IS NOT NULL
              AND t.type IN ('CARD_TRANSACTION', 'CARD_TRANSACTION_INTERNATIONAL')
              AND (:accountId IS NULL OR t.account.id = :accountId)
            GROUP BY t.merchantName
            ORDER BY SUM(t.amount) ASC
            """)
    List<TopMerchantDto> findTopMerchants(@Param("accountId") Long accountId, Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t LEFT JOIN t.counterparty c
            WHERE LOWER(t.merchantName) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<Transaction> findByMerchant(@Param("q") String q, Pageable pageable);

    @Query("SELECT t.transactionId FROM Transaction t WHERE t.transactionId IN :ids")
    Set<UUID> findExistingTransactionIds(@Param("ids") Set<UUID> ids);

    // Retroactive MCC assignment: only touches rows where userCategory is null
    @Modifying
    @Query("UPDATE Transaction t SET t.userCategory.id = :categoryId " +
           "WHERE t.mccCode.mcc = :mcc AND t.userCategory IS NULL")
    int retroactivelyAssignByMcc(@Param("mcc") String mcc, @Param("categoryId") Long categoryId);

    // Bulk category assignment used by transaction-rule retroactive pass
    @Modifying
    @Query("UPDATE Transaction t SET t.userCategory.id = :catId WHERE t.id IN :ids AND t.userCategory IS NULL")
    int bulkAssignCategory(@Param("ids") Collection<Long> ids, @Param("catId") Long catId);

    // Bulk merchant rename used by alias retroactive pass
    @Modifying
    @Query("UPDATE Transaction t SET t.merchantName = :canonical WHERE t.id IN :ids")
    int bulkRenameMerchants(@Param("ids") Collection<Long> ids, @Param("canonical") String canonical);

    // Counterparty-merchant mapping: set merchant_name for all counterparty transactions
    @Modifying
    @Query("UPDATE Transaction t SET t.merchantName = :merchantName WHERE t.counterparty.id = :counterpartyId")
    int bulkSetMerchantNameByCounterparty(@Param("counterpartyId") Long counterpartyId, @Param("merchantName") String merchantName);

    // Counterparty-merchant mapping: clear merchant_name when mapping is deleted
    // Only clears rows where raw_merchant_name is null (counterparty-sourced, not real card merchant transactions)
    @Modifying
    @Query("UPDATE Transaction t SET t.merchantName = NULL WHERE t.counterparty.id = :counterpartyId AND t.rawMerchantName IS NULL AND t.merchantName = :merchantName")
    int bulkClearMerchantNameByCounterparty(@Param("counterpartyId") Long counterpartyId, @Param("merchantName") String merchantName);

    // Candidate load for transaction rule retroactive pass (Sparkasse + TR card payments)
    @Query("""
            SELECT new com.manuelpuchner.backend.transaction.repository.RecategorizationCandidate(
                t.id, t.description, t.merchantName, c.name
            )
            FROM Transaction t LEFT JOIN t.counterparty c
            WHERE (t.source = com.manuelpuchner.backend.transaction.entity.TransactionSource.SPARKASSE
                   OR (t.source = com.manuelpuchner.backend.transaction.entity.TransactionSource.TRADE_REPUBLIC
                       AND t.type IN (com.manuelpuchner.backend.transaction.entity.TransactionType.CARD_TRANSACTION,
                                      com.manuelpuchner.backend.transaction.entity.TransactionType.CARD_TRANSACTION_INTERNATIONAL)))
              AND t.userCategory IS NULL
            """)
    List<RecategorizationCandidate> findTransactionRuleCandidates();

    // Candidate load for asset rule retroactive pass
    @Query("""
            SELECT new com.manuelpuchner.backend.transaction.repository.AssetCandidate(
                t.id, a.symbol, a.name
            )
            FROM Transaction t JOIN t.asset a
            WHERE t.source = com.manuelpuchner.backend.transaction.entity.TransactionSource.TRADE_REPUBLIC
              AND a.assetClass = :assetClass
              AND t.userCategory IS NULL
            """)
    List<AssetCandidate> findAssetCandidatesByClass(@Param("assetClass") com.manuelpuchner.backend.asset.entity.AssetClass assetClass);

    // --- Counterparty queries ---

    @Query("""
            SELECT t.counterparty.id as id, t.counterparty.iban as iban, t.counterparty.name as name,
                   COALESCE(SUM(CASE WHEN t.amount > 0 THEN t.amount ELSE 0 END), 0) as totalIncome,
                   ABS(COALESCE(SUM(CASE WHEN t.amount < 0 THEN t.amount ELSE 0 END), 0)) as totalOutgoing,
                   COALESCE(SUM(t.amount), 0) as net,
                   COUNT(t) as transactionCount
            FROM Transaction t
            WHERE t.counterparty IS NOT NULL
              AND (:q IS NULL OR LOWER(t.counterparty.name) LIKE LOWER(CONCAT('%', :q, '%'))
                              OR LOWER(t.counterparty.iban) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:ibanPrefixPattern IS NULL OR t.counterparty.iban LIKE :ibanPrefixPattern)
            GROUP BY t.counterparty.id, t.counterparty.iban, t.counterparty.name
            ORDER BY ABS(SUM(t.amount)) DESC
            """)
    Page<CounterpartySummaryProjection> searchCounterpartyStats(@Param("q") String q, @Param("ibanPrefixPattern") String ibanPrefixPattern, Pageable pageable);

    @Query("""
            SELECT t.counterparty.id as id, t.counterparty.iban as iban, t.counterparty.name as name,
                   COALESCE(SUM(CASE WHEN t.amount > 0 THEN t.amount ELSE 0 END), 0) as totalIncome,
                   ABS(COALESCE(SUM(CASE WHEN t.amount < 0 THEN t.amount ELSE 0 END), 0)) as totalOutgoing,
                   COALESCE(SUM(t.amount), 0) as net,
                   COUNT(t) as transactionCount
            FROM Transaction t
            WHERE t.counterparty IS NOT NULL
              AND (:ibanPrefixPattern IS NULL OR t.counterparty.iban LIKE :ibanPrefixPattern)
            GROUP BY t.counterparty.id, t.counterparty.iban, t.counterparty.name
            ORDER BY ABS(SUM(t.amount)) DESC
            """)
    Page<CounterpartySummaryProjection> findAllCounterpartyStats(@Param("ibanPrefixPattern") String ibanPrefixPattern, Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.counterparty.id = :counterpartyId
              AND (:accountId IS NULL OR t.account.id = :accountId)
            ORDER BY t.date DESC, t.datetime DESC
            """)
    Page<Transaction> findByCounterpartyId(@Param("counterpartyId") Long counterpartyId,
                                           @Param("accountId") Long accountId, Pageable pageable);

    @Query("""
            SELECT new com.manuelpuchner.backend.common.dto.EntityStatsDto(
                COALESCE(SUM(CASE WHEN t.amount > 0 THEN t.amount ELSE 0 END), 0),
                ABS(COALESCE(SUM(CASE WHEN t.amount < 0 THEN t.amount ELSE 0 END), 0)),
                COALESCE(SUM(t.amount), 0),
                COUNT(t)
            )
            FROM Transaction t
            WHERE t.counterparty.id = :counterpartyId
              AND (:accountId IS NULL OR t.account.id = :accountId)
            """)
    EntityStatsDto statsForCounterparty(@Param("counterpartyId") Long counterpartyId,
                                        @Param("accountId") Long accountId);

    // --- Asset queries ---

    @Query("""
            SELECT t.asset.id as id, t.asset.symbol as symbol, t.asset.name as name, t.asset.assetClass as assetClass,
                   COALESCE(SUM(CASE WHEN t.amount > 0 THEN t.amount ELSE 0 END), 0) as totalIncome,
                   ABS(COALESCE(SUM(CASE WHEN t.amount < 0 THEN t.amount ELSE 0 END), 0)) as totalOutgoing,
                   COALESCE(SUM(t.amount) - ABS(SUM(t.fee)), 0) as net,
                   COUNT(t) as transactionCount
            FROM Transaction t
            WHERE t.asset IS NOT NULL
              AND (:q IS NULL OR LOWER(t.asset.name) LIKE LOWER(CONCAT('%', :q, '%'))
                              OR LOWER(t.asset.symbol) LIKE LOWER(CONCAT('%', :q, '%')))
            GROUP BY t.asset.id, t.asset.symbol, t.asset.name, t.asset.assetClass
            ORDER BY ABS(SUM(t.amount)) DESC
            """)
    Page<AssetSummaryProjection> searchAssetStats(@Param("q") String q, Pageable pageable);

    @Query("""
            SELECT t.asset.id as id, t.asset.symbol as symbol, t.asset.name as name, t.asset.assetClass as assetClass,
                   COALESCE(SUM(CASE WHEN t.amount > 0 THEN t.amount ELSE 0 END), 0) as totalIncome,
                   ABS(COALESCE(SUM(CASE WHEN t.amount < 0 THEN t.amount ELSE 0 END), 0)) as totalOutgoing,
                   COALESCE(SUM(t.amount) - ABS(SUM(t.fee)), 0) as net,
                   COUNT(t) as transactionCount
            FROM Transaction t
            WHERE t.asset IS NOT NULL
            GROUP BY t.asset.id, t.asset.symbol, t.asset.name, t.asset.assetClass
            ORDER BY ABS(SUM(t.amount)) DESC
            """)
    Page<AssetSummaryProjection> findAllAssetStats(Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.asset.id = :assetId
              AND (:accountId IS NULL OR t.account.id = :accountId)
            ORDER BY t.date DESC, t.datetime DESC
            """)
    Page<Transaction> findByAssetId(@Param("assetId") Long assetId,
                                    @Param("accountId") Long accountId, Pageable pageable);

    @Query("""
            SELECT new com.manuelpuchner.backend.common.dto.EntityStatsDto(
                COALESCE(SUM(CASE WHEN t.amount > 0 THEN t.amount ELSE 0 END), 0),
                ABS(COALESCE(SUM(CASE WHEN t.amount < 0 THEN t.amount ELSE 0 END), 0)),
                COALESCE(SUM(t.amount) - ABS(SUM(t.fee)), 0),
                COUNT(t)
            )
            FROM Transaction t
            WHERE t.asset.id = :assetId
              AND (:accountId IS NULL OR t.account.id = :accountId)
            """)
    EntityStatsDto statsForAsset(@Param("assetId") Long assetId,
                                 @Param("accountId") Long accountId);

    // --- Merchant queries ---

    @Query("""
            SELECT t.merchantName as name,
                   COALESCE(SUM(CASE WHEN t.amount > 0 THEN t.amount ELSE 0 END), 0) as totalIncome,
                   ABS(COALESCE(SUM(CASE WHEN t.amount < 0 THEN t.amount ELSE 0 END), 0)) as totalOutgoing,
                   COALESCE(SUM(t.amount), 0) as net,
                   COUNT(t) as transactionCount
            FROM Transaction t
            WHERE t.merchantName IS NOT NULL
              AND (:q IS NULL OR LOWER(t.merchantName) LIKE LOWER(CONCAT('%', :q, '%')))
            GROUP BY t.merchantName
            ORDER BY ABS(SUM(t.amount)) DESC
            """)
    Page<MerchantSummaryProjection> searchMerchantStats(@Param("q") String q, Pageable pageable);

    @Query("""
            SELECT t.merchantName as name,
                   COALESCE(SUM(CASE WHEN t.amount > 0 THEN t.amount ELSE 0 END), 0) as totalIncome,
                   ABS(COALESCE(SUM(CASE WHEN t.amount < 0 THEN t.amount ELSE 0 END), 0)) as totalOutgoing,
                   COALESCE(SUM(t.amount), 0) as net,
                   COUNT(t) as transactionCount
            FROM Transaction t
            WHERE t.merchantName IS NOT NULL
            GROUP BY t.merchantName
            ORDER BY ABS(SUM(t.amount)) DESC
            """)
    Page<MerchantSummaryProjection> findAllMerchantStats(Pageable pageable);

    @Query("""
            SELECT new com.manuelpuchner.backend.common.dto.EntityStatsDto(
                COALESCE(SUM(CASE WHEN t.amount > 0 THEN t.amount ELSE 0 END), 0),
                ABS(COALESCE(SUM(CASE WHEN t.amount < 0 THEN t.amount ELSE 0 END), 0)),
                COALESCE(SUM(t.amount), 0),
                COUNT(t)
            )
            FROM Transaction t
            WHERE t.merchantName = :name
              AND (:accountId IS NULL OR t.account.id = :accountId)
            """)
    EntityStatsDto statsForMerchant(@Param("name") String name, @Param("accountId") Long accountId);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.merchantName = :name
              AND (:accountId IS NULL OR t.account.id = :accountId)
            ORDER BY t.date DESC, t.datetime DESC
            """)
    Page<Transaction> findByMerchantName(@Param("name") String name,
                                         @Param("accountId") Long accountId, Pageable pageable);

    // Candidate load for merchant alias retroactive pass
    @Query("""
            SELECT new com.manuelpuchner.backend.transaction.repository.RecategorizationCandidate(
                t.id, t.description, t.merchantName, c.name
            )
            FROM Transaction t LEFT JOIN t.counterparty c
            WHERE t.merchantName IS NOT NULL
            """)
    List<RecategorizationCandidate> findAllMerchantCandidates();
}
