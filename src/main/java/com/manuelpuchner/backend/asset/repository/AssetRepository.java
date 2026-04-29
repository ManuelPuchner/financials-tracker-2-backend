package com.manuelpuchner.backend.asset.repository;

import com.manuelpuchner.backend.asset.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    Optional<Asset> findBySymbol(String symbol);

    Set<Asset> findBySymbolIn(Set<String> symbols);

    default Map<String, Asset> findBySymbolsAsMap(Set<String> symbols) {
        return findBySymbolIn(symbols).stream()
                .collect(Collectors.toMap(Asset::getSymbol, a -> a));
    }
}
