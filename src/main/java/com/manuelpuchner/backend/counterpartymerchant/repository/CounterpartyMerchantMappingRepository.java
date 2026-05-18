package com.manuelpuchner.backend.counterpartymerchant.repository;

import com.manuelpuchner.backend.counterpartymerchant.entity.CounterpartyMerchantMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public interface CounterpartyMerchantMappingRepository extends JpaRepository<CounterpartyMerchantMapping, Long> {

    Optional<CounterpartyMerchantMapping> findByCounterparty_Id(Long counterpartyId);

    Set<CounterpartyMerchantMapping> findByCounterparty_IdIn(Set<Long> counterpartyIds);

    default Map<Long, String> findMerchantNamesByCounterpartyIds(Set<Long> counterpartyIds) {
        return findByCounterparty_IdIn(counterpartyIds).stream()
                .collect(Collectors.toMap(m -> m.getCounterparty().getId(), CounterpartyMerchantMapping::getMerchantName));
    }
}
