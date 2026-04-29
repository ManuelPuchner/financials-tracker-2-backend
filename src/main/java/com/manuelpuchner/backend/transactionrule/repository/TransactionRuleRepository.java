package com.manuelpuchner.backend.transactionrule.repository;

import com.manuelpuchner.backend.transactionrule.entity.TransactionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRuleRepository extends JpaRepository<TransactionRule, Long> {

    List<TransactionRule> findAllByOrderByPriorityAscIdAsc();
}
