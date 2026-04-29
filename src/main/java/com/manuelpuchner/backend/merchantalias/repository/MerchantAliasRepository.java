package com.manuelpuchner.backend.merchantalias.repository;

import com.manuelpuchner.backend.merchantalias.entity.MerchantAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantAliasRepository extends JpaRepository<MerchantAlias, Long> {
}
