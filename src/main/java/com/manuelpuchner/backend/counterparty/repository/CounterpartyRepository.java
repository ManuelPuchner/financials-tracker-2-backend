package com.manuelpuchner.backend.counterparty.repository;

import com.manuelpuchner.backend.counterparty.entity.Counterparty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public interface CounterpartyRepository extends JpaRepository<Counterparty, Long> {

    Optional<Counterparty> findByIban(String iban);

    Set<Counterparty> findByIbanIn(Set<String> ibans);

    default Map<String, Counterparty> findByIbansAsMap(Set<String> ibans) {
        return findByIbanIn(ibans).stream()
                .collect(Collectors.toMap(Counterparty::getIban, c -> c));
    }
}
