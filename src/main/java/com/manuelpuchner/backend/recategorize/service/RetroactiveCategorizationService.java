package com.manuelpuchner.backend.recategorize.service;

import com.manuelpuchner.backend.assetrule.entity.AssetRule;
import com.manuelpuchner.backend.assetrule.entity.AssetRuleTargetField;
import com.manuelpuchner.backend.merchantalias.entity.MerchantAlias;
import com.manuelpuchner.backend.transactionrule.entity.RuleTargetField;
import com.manuelpuchner.backend.transactionrule.entity.TransactionRule;
import com.manuelpuchner.backend.transaction.repository.AssetCandidate;
import com.manuelpuchner.backend.transaction.repository.TransactionRepository;
import com.manuelpuchner.backend.transaction.repository.RecategorizationCandidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetroactiveCategorizationService {

    private final TransactionRepository transactionRepository;

    @Transactional
    public void applyMccMapping(String mcc, Long categoryId) {
        if (categoryId == null) return;
        int updated = transactionRepository.retroactivelyAssignByMcc(mcc, categoryId);
        log.info("[Retroactive] MCC={} categoryId={} updated={}", mcc, categoryId, updated);
    }

    @Transactional
    public void applyTransactionRule(TransactionRule rule) {
        List<RecategorizationCandidate> candidates = transactionRepository.findTransactionRuleCandidates();
        Pattern compiled = Pattern.compile(rule.getPattern());
        Long catId = rule.getUserCategory().getId();

        List<Long> matchedIds = new ArrayList<>();
        for (RecategorizationCandidate c : candidates) {
            if (matches(compiled, rule.getTargetField(), c)) {
                matchedIds.add(c.id());
            }
        }

        if (!matchedIds.isEmpty()) {
            int updated = transactionRepository.bulkAssignCategory(matchedIds, catId);
            log.info("[Retroactive] TransactionRule id={} categoryId={} updated={}", rule.getId(), catId, updated);
        }
    }

    @Transactional
    public void applyAssetRule(AssetRule rule) {
        List<AssetCandidate> candidates = transactionRepository.findAssetCandidatesByClass(rule.getAssetClass());
        Pattern compiled = Pattern.compile(rule.getPattern());
        Long catId = rule.getUserCategory().getId();

        List<Long> matchedIds = new ArrayList<>();
        for (AssetCandidate c : candidates) {
            if (matchesAsset(compiled, rule.getTargetField(), c)) {
                matchedIds.add(c.id());
            }
        }

        if (!matchedIds.isEmpty()) {
            int updated = transactionRepository.bulkAssignCategory(matchedIds, catId);
            log.info("[Retroactive] AssetRule id={} assetClass={} categoryId={} updated={}",
                    rule.getId(), rule.getAssetClass(), catId, updated);
        }
    }

    @Transactional
    public void applyMerchantAlias(MerchantAlias alias) {
        List<RecategorizationCandidate> candidates = transactionRepository.findAllMerchantCandidates();
        Pattern compiled = Pattern.compile(alias.getPattern());

        List<Long> matchedIds = new ArrayList<>();
        for (RecategorizationCandidate c : candidates) {
            String merchantName = c.merchantName();
            if (merchantName != null && compiled.matcher(merchantName).find()
                    && !alias.getCanonicalName().equals(merchantName)) {
                matchedIds.add(c.id());
            }
        }

        if (!matchedIds.isEmpty()) {
            int updated = transactionRepository.bulkRenameMerchants(matchedIds, alias.getCanonicalName());
            log.info("[Retroactive] MerchantAlias id={} canonical='{}' updated={}", alias.getId(), alias.getCanonicalName(), updated);
        }
    }

    private boolean matchesAsset(Pattern pattern, AssetRuleTargetField targetField, AssetCandidate c) {
        return switch (targetField) {
            case SYMBOL -> c.symbol() != null && pattern.matcher(c.symbol()).find();
            case NAME -> c.name() != null && pattern.matcher(c.name()).find();
            case BOTH -> (c.symbol() != null && pattern.matcher(c.symbol()).find())
                    || (c.name() != null && pattern.matcher(c.name()).find());
        };
    }

    private boolean matches(Pattern pattern, RuleTargetField targetField, RecategorizationCandidate c) {
        String partnerField = c.merchantName() != null ? c.merchantName() : c.counterpartyName();
        String counterpartyName = c.counterpartyName();
        String reference = c.description();
        return switch (targetField) {
            case PARTNER_NAME -> partnerField != null && pattern.matcher(partnerField).find();
            case COUNTERPARTY_NAME -> counterpartyName != null && pattern.matcher(counterpartyName).find();
            case REFERENCE -> reference != null && pattern.matcher(reference).find();
            case BOTH -> (partnerField != null && pattern.matcher(partnerField).find())
                    || (counterpartyName != null && pattern.matcher(counterpartyName).find())
                    || (reference != null && pattern.matcher(reference).find());
        };
    }
}
