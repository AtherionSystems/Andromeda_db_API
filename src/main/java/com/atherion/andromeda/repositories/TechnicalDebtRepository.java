package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.TechnicalDebt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TechnicalDebtRepository extends JpaRepository<TechnicalDebt, Long> {
    List<TechnicalDebt> findByProject_Id(Long projectId);
    List<TechnicalDebt> findByStatus(String status);
    List<TechnicalDebt> findByAssignedTo_Id(Long userId);
}
