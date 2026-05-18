package com.manuelpuchner.backend.transaction.service;

import com.manuelpuchner.backend.account.entity.Account;
import com.manuelpuchner.backend.account.repository.AccountRepository;
import com.manuelpuchner.backend.account.service.AccountService;
import com.manuelpuchner.backend.asset.entity.Asset;
import com.manuelpuchner.backend.asset.repository.AssetRepository;
import com.manuelpuchner.backend.counterparty.entity.Counterparty;
import com.manuelpuchner.backend.counterparty.repository.CounterpartyRepository;
import com.manuelpuchner.backend.mcc.entity.MccCode;
import com.manuelpuchner.backend.mcc.repository.MccCodeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manuelpuchner.backend.assetrule.service.AssetRuleService;
import com.manuelpuchner.backend.counterpartymerchant.service.CounterpartyMerchantMappingService;
import com.manuelpuchner.backend.merchantalias.service.MerchantAliasService;
import com.manuelpuchner.backend.transactionrule.service.TransactionRuleService;
import com.manuelpuchner.backend.transaction.dto.*;
import com.manuelpuchner.backend.transaction.entity.*;
import com.manuelpuchner.backend.transaction.repository.TransactionRepository;
import com.manuelpuchner.backend.usercategory.entity.UserCategory;
import com.manuelpuchner.backend.usercategory.repository.UserCategoryRepository;
import com.opencsv.exceptions.CsvException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AssetRepository assetRepository;
    private final CounterpartyRepository counterpartyRepository;
    private final MccCodeRepository mccCodeRepository;
    private final AccountRepository accountRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final TransactionMapper mapper;
    private final TransactionCsvParser csvParser;
    private final ObjectMapper objectMapper;
    private final AccountService accountService;
    private final MerchantAliasService merchantAliasService;
    private final CounterpartyMerchantMappingService counterpartyMerchantMappingService;
    private final TransactionRuleService transactionRuleService;
    private final AssetRuleService assetRuleService;

    @Transactional(readOnly = true)
    public Page<TransactionResponse> findAll(Pageable pageable) {
        return transactionRepository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TransactionResponse findByTransactionId(UUID transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionId));
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> findByCategory(Category category, Pageable pageable) {
        return transactionRepository.findByCategory(category, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> findByType(TransactionType type, Pageable pageable) {
        return transactionRepository.findByType(type, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> findByMccCode(String mcc, Pageable pageable) {
        return transactionRepository.findByMccCode_Mcc(mcc, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> findByDateRange(LocalDate from, LocalDate to, Pageable pageable) {
        return transactionRepository.findByDateBetween(from, to, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> findByMerchant(String q, Pageable pageable) {
        return transactionRepository.findByMerchant(q, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> findByUserCategoryId(Long userCategoryId, Pageable pageable) {
        return transactionRepository.findByUserCategory_Id(userCategoryId, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> findByAccountId(Long accountId, Pageable pageable) {
        return transactionRepository.findByAccount_Id(accountId, pageable).map(mapper::toResponse);
    }

    @Transactional
    public TransactionResponse create(TransactionRequest req) {
        if (transactionRepository.existsByTransactionId(req.transactionId())) {
            throw new IllegalArgumentException("Transaction already exists: " + req.transactionId());
        }

        Asset asset = resolveAssetFromRequest(req);
        Counterparty counterparty = resolveCounterpartyFromRequest(req);

        FxInfo fxInfo = req.fxOriginalCurrency() != null
                ? new FxInfo(req.fxOriginalAmount(), req.fxOriginalCurrency(), req.fxRate())
                : null;

        MccCode mccCode = req.mccCode() != null
                ? mccCodeRepository.findById(req.mccCode()).orElse(null)
                : null;

        String rawMerchantName = req.merchantName();
        String merchantName = applyAlias(rawMerchantName);
        UserCategory userCategory = mccCode != null ? mccCode.getUserCategory() : null;

        Account account = req.accountId() != null
                ? accountRepository.findById(req.accountId())
                        .orElseThrow(() -> new EntityNotFoundException("Account not found: " + req.accountId()))
                : accountService.resolveOrCreate(TransactionSource.TRADE_REPUBLIC, null);

        TransactionSource source = req.transactionSource() != null
                ? req.transactionSource()
                : account.getSource();

        Transaction transaction = Transaction.builder()
                .transactionId(req.transactionId())
                .source(source)
                .datetime(req.datetime())
                .date(req.date())
                .accountType(req.accountType())
                .category(req.category())
                .type(req.type())
                .amount(req.amount())
                .fee(req.fee())
                .tax(req.tax())
                .currency(req.currency())
                .description(req.description())
                .asset(asset)
                .shares(req.shares())
                .price(req.price())
                .counterparty(counterparty)
                .paymentReference(req.paymentReference())
                .merchantName(merchantName)
                .rawMerchantName(rawMerchantName)
                .fxInfo(fxInfo)
                .mccCode(mccCode)
                .userCategory(userCategory)
                .account(account)
                .build();

        return mapper.toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse assignUserCategory(UUID transactionId, Long categoryId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionId));

        UserCategory category = categoryId != null
                ? userCategoryRepository.findById(categoryId)
                        .orElseThrow(() -> new EntityNotFoundException("Category not found: " + categoryId))
                : null;

        transaction.setUserCategory(category);
        return mapper.toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse update(UUID transactionId, TransactionUpdateRequest req) {
        Transaction tx = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionId));

        // PATCH semantics: null = clear field, value = set field, absent field means caller sends null anyway
        tx.setNote(req.note());
        if (req.description() != null) tx.setDescription(req.description());
        if (req.merchantName() != null) tx.setMerchantName(req.merchantName());
        if (req.category() != null) tx.setCategory(req.category());

        if (req.mccCode() != null) {
            MccCode mcc = mccCodeRepository.findById(req.mccCode())
                    .orElseThrow(() -> new EntityNotFoundException("MCC not found: " + req.mccCode()));
            tx.setMccCode(mcc);
        }

        // null userCategoryId = clear category
        if (req.userCategoryId() != null) {
            UserCategory uc = userCategoryRepository.findById(req.userCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found: " + req.userCategoryId()));
            tx.setUserCategory(uc);
        } else {
            tx.setUserCategory(null);
        }

        return mapper.toResponse(transactionRepository.save(tx));
    }

    @Transactional
    public void delete(UUID transactionId) {
        Transaction tx = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionId));
        transactionRepository.delete(tx);
    }

    private String applyAlias(String merchantName) {
        if (merchantName == null) return null;
        return merchantAliasService.resolve(merchantName).orElse(merchantName);
    }

    private Asset resolveAssetFromRequest(TransactionRequest req) {
        if (req.assetSymbol() == null) return null;
        return assetRepository.findBySymbol(req.assetSymbol())
                .orElseGet(() -> assetRepository.save(Asset.builder()
                        .symbol(req.assetSymbol())
                        .name(req.assetName() != null ? req.assetName() : req.assetSymbol())
                        .assetClass(req.assetClass())
                        .build()));
    }

    private Counterparty resolveCounterpartyFromRequest(TransactionRequest req) {
        if (req.counterpartyIban() == null) return null;
        return counterpartyRepository.findByIban(req.counterpartyIban())
                .orElseGet(() -> counterpartyRepository.save(Counterparty.builder()
                        .iban(req.counterpartyIban())
                        .name(req.counterpartyName())
                        .build()));
    }

    @Transactional
    public CsvImportResult importCsv(Reader reader) throws IOException, CsvException {
        List<CsvRow> rows = csvParser.parse(reader);
        if (rows.isEmpty()) {
            return CsvImportResult.builder().total(0).imported(0).skipped(0).build();
        }

        Set<UUID> incomingIds = rows.stream().map(CsvRow::transactionId).collect(Collectors.toSet());
        Set<UUID> existingIds = transactionRepository.findExistingTransactionIds(incomingIds);
        List<CsvRow> newRows = rows.stream()
                .filter(r -> !existingIds.contains(r.transactionId()))
                .toList();

        if (!newRows.isEmpty()) {
            Map<String, Asset> assetCache = resolveAssets(newRows);
            Map<String, Counterparty> counterpartyCache = resolveCounterparties(newRows);
            Map<String, MccCode> mccCache = resolveMccCodes(newRows);
            Account trAccount = accountService.resolveOrCreate(TransactionSource.TRADE_REPUBLIC, null);

            List<Transaction> toSave = newRows.stream()
                    .map(r -> toEntity(r, assetCache, counterpartyCache, mccCache, trAccount))
                    .toList();

            transactionRepository.saveAll(toSave);
        }

        return CsvImportResult.builder()
                .total(rows.size())
                .imported(newRows.size())
                .skipped(rows.size() - newRows.size())
                .build();
    }

    @Transactional
    public CsvImportResult importSparkasseJson(java.io.InputStream inputStream) throws IOException {
        List<SparkasseTransactionDto> rows = objectMapper.readValue(
                inputStream, new TypeReference<List<SparkasseTransactionDto>>() {});

        if (rows.isEmpty()) {
            return CsvImportResult.builder().total(0).imported(0).skipped(0).build();
        }

        List<SparkasseTransactionDto> validRows = rows.stream()
                .filter(r -> {
                    if (r.getAmount() == null) {
                        log.info("[Sparkasse skip] null amount — reference={} partner={} booking={}",
                                r.getReferenceNumber(), r.getPartnerName(), r.getBooking());
                        return false;
                    }
                    BigDecimal amount = r.getAmount().toBigDecimal();
                    if (amount == null || amount.signum() == 0) {
                        log.info("[Sparkasse skip] zero amount — reference={} partner={} booking={} description={}",
                                r.getReferenceNumber(), r.getPartnerName(), r.getBooking(), r.getReference());
                        return false;
                    }
                    return true;
                })
                .toList();
        int skippedZero = rows.size() - validRows.size();

        Map<UUID, SparkasseTransactionDto> byUuid = new LinkedHashMap<>();
        for (SparkasseTransactionDto row : validRows) {
            UUID id = deterministicUuid(row);
            byUuid.putIfAbsent(id, row);
        }

        Set<UUID> existingIds = transactionRepository.findExistingTransactionIds(byUuid.keySet());
        existingIds.forEach(id -> {
            SparkasseTransactionDto dup = byUuid.get(id);
            log.info("[Sparkasse skip] duplicate uuid={} reference={} partner={} booking={}",
                    id, dup.getReferenceNumber(), dup.getPartnerName(), dup.getBooking());
        });
        Map<UUID, SparkasseTransactionDto> newRows = byUuid.entrySet().stream()
                .filter(e -> !existingIds.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

        if (!newRows.isEmpty()) {
            Map<String, Counterparty> counterpartyCache = resolveSparkasseCounterparties(newRows.values());

            // Group rows by owner IBAN to resolve accounts per IBAN
            Map<String, Account> accountCache = newRows.values().stream()
                    .map(SparkasseTransactionDto::getOwnerAccountNumber)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toMap(
                            iban -> iban,
                            iban -> accountService.resolveOrCreate(TransactionSource.SPARKASSE, iban)
                    ));
            // Fallback for rows without IBAN
            Account fallbackAccount = accountService.resolveOrCreate(TransactionSource.SPARKASSE, null);

            List<Transaction> toSave = newRows.entrySet().stream()
                    .map(e -> toSparkasseEntity(e.getKey(), e.getValue(), counterpartyCache, accountCache, fallbackAccount))
                    .toList();

            transactionRepository.saveAll(toSave);
        }

        int imported = newRows.size();
        int skipped = skippedZero + (validRows.size() - imported);
        log.info("[Sparkasse import] total={} imported={} skipped={} (zeroAmount={} duplicates={})",
                rows.size(), imported, skipped, skippedZero, existingIds.size());
        return CsvImportResult.builder()
                .total(rows.size())
                .imported(imported)
                .skipped(skipped)
                .build();
    }

    private UUID deterministicUuid(SparkasseTransactionDto row) {
        String key;
        if (row.getReferenceNumber() != null && !row.getReferenceNumber().isBlank()) {
            key = "SPARKASSE|REF|" + row.getOwnerAccountNumber() + "|" + row.getReferenceNumber();
        } else {
            String partnerIban = row.getPartnerAccount() != null ? row.getPartnerAccount().getIban() : null;
            BigDecimal amount = row.getAmount() != null ? row.getAmount().toBigDecimal() : null;
            key = "SPARKASSE|HASH|"
                    + nullSafe(row.getOwnerAccountNumber()) + "|"
                    + nullSafe(row.getBooking() != null ? row.getBooking().toString() : null) + "|"
                    + (amount != null ? amount.toPlainString() : "") + "|"
                    + nullSafe(partnerIban) + "|"
                    + nullSafe(row.getPartnerName()) + "|"
                    + nullSafe(row.getReference());
        }
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }

    private Map<String, Counterparty> resolveSparkasseCounterparties(Collection<SparkasseTransactionDto> rows) {
        Set<String> ibans = rows.stream()
                .filter(r -> r.getPartnerAccount() != null)
                .map(r -> r.getPartnerAccount().getIban())
                .filter(iban -> iban != null && !iban.isBlank())
                .collect(Collectors.toSet());
        if (ibans.isEmpty()) return Map.of();

        Map<String, Counterparty> existing = counterpartyRepository.findByIbansAsMap(ibans);

        List<Counterparty> toCreate = rows.stream()
                .filter(r -> r.getPartnerAccount() != null
                        && r.getPartnerAccount().getIban() != null
                        && !r.getPartnerAccount().getIban().isBlank()
                        && !existing.containsKey(r.getPartnerAccount().getIban()))
                .collect(Collectors.toMap(
                        r -> r.getPartnerAccount().getIban(),
                        r -> r,
                        (a, b) -> a))
                .values().stream()
                .map(r -> Counterparty.builder()
                        .iban(r.getPartnerAccount().getIban())
                        .name(r.getPartnerName())
                        .build())
                .toList();

        if (!toCreate.isEmpty()) {
            counterpartyRepository.saveAll(toCreate).forEach(c -> existing.put(c.getIban(), c));
        }

        return existing;
    }

    private Transaction toSparkasseEntity(UUID transactionId, SparkasseTransactionDto r,
                                          Map<String, Counterparty> counterparties,
                                          Map<String, Account> accountCache,
                                          Account fallbackAccount) {
        BigDecimal amount = r.getAmount() != null ? r.getAmount().toBigDecimal() : null;
        String currency = r.getAmount() != null && r.getAmount().getCurrency() != null
                ? r.getAmount().getCurrency().toUpperCase()
                : "EUR";

        TransactionType type = SparkasseTypeInferer.infer(r, amount);
        boolean isCard = SparkasseTypeInferer.isCardTransaction(r);

        String rawMerchantName = null;
        Counterparty counterparty = null;
        String partnerNameForRule = null;
        String counterpartyNameForRule = null;

        if (isCard) {
            rawMerchantName = r.getMerchantName() != null ? r.getMerchantName() : r.getPartnerName();
            partnerNameForRule = rawMerchantName;
        } else if (r.getPartnerAccount() != null
                && r.getPartnerAccount().getIban() != null
                && !r.getPartnerAccount().getIban().isBlank()) {
            counterparty = counterparties.get(r.getPartnerAccount().getIban());
            partnerNameForRule = r.getPartnerName();
            counterpartyNameForRule = r.getPartnerName();
        } else {
            partnerNameForRule = r.getPartnerName();
        }

        // 1. Apply merchant alias (rawMerchantName preserved as-is)
        String merchantName = applyAlias(rawMerchantName);

        // 2. If no merchant name resolved and a counterparty merchant mapping exists, use it
        if (merchantName == null && counterparty != null) {
            merchantName = counterpartyMerchantMappingService.resolveMerchantName(counterparty.getId()).orElse(null);
        }

        // 2. Apply transaction rule if no category yet
        UserCategory userCategory = transactionRuleService
                .firstMatch(partnerNameForRule, counterpartyNameForRule, r.getReference())
                .orElse(null);

        OffsetDateTime bookingOdt = r.getBooking() != null ? r.getBooking()
                : (r.getValuation() != null ? r.getValuation() : OffsetDateTime.now());
        Instant datetime = bookingOdt.toInstant();
        LocalDate date = datetime.atZone(java.time.ZoneId.of("Europe/Vienna")).toLocalDate();
        log.debug("[Sparkasse date] bookingOdt={} datetime={} date={}", bookingOdt, datetime, date);

        Account account = r.getOwnerAccountNumber() != null
                ? accountCache.getOrDefault(r.getOwnerAccountNumber(), fallbackAccount)
                : fallbackAccount;

        return Transaction.builder()
                .transactionId(transactionId)
                .source(TransactionSource.SPARKASSE)
                .datetime(datetime)
                .date(date)
                .accountType(AccountType.DEFAULT)
                .category(Category.CASH)
                .type(type)
                .amount(amount)
                .currency(currency)
                .description(r.getReference())
                .note(r.getNote())
                .merchantName(merchantName)
                .rawMerchantName(rawMerchantName)
                .counterparty(counterparty)
                .paymentReference(r.getReferenceNumber() != null ? r.getReferenceNumber() : r.getReference())
                .ownAccountIban(r.getOwnerAccountNumber())
                .ownAccountName(r.getOwnerAccountTitle())
                .sepaMandateId(r.getSepaMandateId())
                .sepaCreditorId(r.getSepaCreditorId())
                .paymentMethod(r.getPaymentMethod())
                .receiverReference(r.getReceiverReference())
                .userCategory(userCategory)
                .account(account)
                .build();
    }

    private Map<String, Asset> resolveAssets(List<CsvRow> rows) {
        Set<String> symbols = rows.stream()
                .map(CsvRow::assetSymbol)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (symbols.isEmpty()) return Map.of();

        Map<String, Asset> existing = assetRepository.findBySymbolsAsMap(symbols);

        List<Asset> toCreate = rows.stream()
                .filter(r -> r.assetSymbol() != null && !existing.containsKey(r.assetSymbol()))
                .collect(Collectors.toMap(CsvRow::assetSymbol, r -> r, (a, b) -> a))
                .values().stream()
                .map(r -> Asset.builder()
                        .symbol(r.assetSymbol())
                        .name(r.assetName() != null ? r.assetName() : r.assetSymbol())
                        .assetClass(r.assetClass())
                        .build())
                .toList();

        if (!toCreate.isEmpty()) {
            assetRepository.saveAll(toCreate).forEach(a -> existing.put(a.getSymbol(), a));
        }

        return existing;
    }

    private Map<String, Counterparty> resolveCounterparties(List<CsvRow> rows) {
        Set<String> ibans = rows.stream()
                .map(CsvRow::counterpartyIban)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ibans.isEmpty()) return Map.of();

        Map<String, Counterparty> existing = counterpartyRepository.findByIbansAsMap(ibans);

        List<Counterparty> toCreate = rows.stream()
                .filter(r -> r.counterpartyIban() != null && !existing.containsKey(r.counterpartyIban()))
                .collect(Collectors.toMap(CsvRow::counterpartyIban, r -> r, (a, b) -> a))
                .values().stream()
                .map(r -> Counterparty.builder()
                        .iban(r.counterpartyIban())
                        .name(r.counterpartyName())
                        .build())
                .toList();

        if (!toCreate.isEmpty()) {
            counterpartyRepository.saveAll(toCreate).forEach(c -> existing.put(c.getIban(), c));
        }

        return existing;
    }

    private Map<String, MccCode> resolveMccCodes(List<CsvRow> rows) {
        Set<String> codes = rows.stream()
                .map(CsvRow::mccCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (codes.isEmpty()) return Map.of();
        return mccCodeRepository.findAllById(codes).stream()
                .collect(Collectors.toMap(MccCode::getMcc, m -> m));
    }

    private Transaction toEntity(CsvRow r, Map<String, Asset> assets, Map<String, Counterparty> counterparties,
                                 Map<String, MccCode> mccCodes, Account account) {
        FxInfo fxInfo = r.fxOriginalCurrency() != null
                ? new FxInfo(r.fxOriginalAmount(), r.fxOriginalCurrency(), r.fxRate())
                : null;

        MccCode mcc = r.mccCode() != null ? mccCodes.get(r.mccCode()) : null;
        String rawMerchantNameCsv = r.merchantName();
        String merchantName = applyAlias(rawMerchantNameCsv);
        Asset asset = r.assetSymbol() != null ? assets.get(r.assetSymbol()) : null;

        boolean isCard = r.type() == TransactionType.CARD_TRANSACTION
                || r.type() == TransactionType.CARD_TRANSACTION_INTERNATIONAL;

        UserCategory userCategory;
        if (isCard) {
            // transaction rules override MCC for card payments
            userCategory = transactionRuleService
                    .firstMatch(merchantName, null, r.description())
                    .orElseGet(() -> mcc != null ? mcc.getUserCategory() : null);
        } else if (asset != null && asset.getAssetClass() != null) {
            // Asset rules for investment transactions; excluded from transaction rules
            userCategory = assetRuleService
                    .firstMatch(asset.getAssetClass(), asset.getSymbol(), asset.getName())
                    .orElse(null);
        } else {
            userCategory = mcc != null ? mcc.getUserCategory() : null;
        }

        return Transaction.builder()
                .transactionId(r.transactionId())
                .source(TransactionSource.TRADE_REPUBLIC)
                .datetime(r.datetime())
                .date(r.date())
                .accountType(r.accountType())
                .category(r.category())
                .type(r.type())
                .amount(r.amount())
                .fee(r.fee())
                .tax(r.tax())
                .currency(r.currency())
                .description(r.description())
                .asset(asset)
                .shares(r.shares())
                .price(r.price())
                .counterparty(r.counterpartyIban() != null ? counterparties.get(r.counterpartyIban()) : null)
                .paymentReference(r.paymentReference())
                .merchantName(merchantName)
                .rawMerchantName(rawMerchantNameCsv)
                .fxInfo(fxInfo)
                .mccCode(mcc)
                .userCategory(userCategory)
                .account(account)
                .build();
    }
}
