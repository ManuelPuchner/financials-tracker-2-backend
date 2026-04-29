package com.manuelpuchner.backend.sparkasserule.repository;

import com.manuelpuchner.backend.sparkasserule.entity.SparkasseRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SparkasseRuleRepository extends JpaRepository<SparkasseRule, Long> {

    List<SparkasseRule> findAllByOrderByPriorityAscIdAsc();
}
