package com.manuelpuchner.backend.assetrule.repository;

import com.manuelpuchner.backend.asset.entity.AssetClass;
import com.manuelpuchner.backend.assetrule.entity.AssetRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetRuleRepository extends JpaRepository<AssetRule, Long> {
    List<AssetRule> findAllByOrderByPriorityAscIdAsc();
    List<AssetRule> findAllByAssetClassOrderByPriorityAscIdAsc(AssetClass assetClass);
}
