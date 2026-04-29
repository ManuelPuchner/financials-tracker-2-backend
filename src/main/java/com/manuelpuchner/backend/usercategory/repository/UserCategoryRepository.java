package com.manuelpuchner.backend.usercategory.repository;

import com.manuelpuchner.backend.usercategory.entity.UserCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCategoryRepository extends JpaRepository<UserCategory, Long> {
    boolean existsByNameIgnoreCase(String name);
}
