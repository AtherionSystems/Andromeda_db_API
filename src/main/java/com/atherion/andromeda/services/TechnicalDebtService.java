package com.atherion.andromeda.services;

import com.atherion.andromeda.model.TechnicalDebt;
import com.atherion.andromeda.repositories.TechnicalDebtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TechnicalDebtService {
    private final TechnicalDebtRepository technicalDebtRepository;

    public List<TechnicalDebt> findAll() { return technicalDebtRepository.findAll(); }
    public List<TechnicalDebt> findByProjectId(Long projectId) { return technicalDebtRepository.findByProject_Id(projectId); }
    public List<TechnicalDebt> findByStatus(String status) { return technicalDebtRepository.findByStatus(status); }
    public List<TechnicalDebt> findByAssignedToId(Long userId) { return technicalDebtRepository.findByAssignedTo_Id(userId); }
    public Optional<TechnicalDebt> findById(Long id) { return technicalDebtRepository.findById(id); }
    public TechnicalDebt save(TechnicalDebt technicalDebt) { return technicalDebtRepository.save(technicalDebt); }
    public void deleteById(Long id) { technicalDebtRepository.deleteById(id); }
}
