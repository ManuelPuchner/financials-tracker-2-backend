package com.manuelpuchner.backend.mcc.repository;

import com.manuelpuchner.backend.mcc.entity.MccCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MccCodeRepository extends JpaRepository<MccCode, String> {}
