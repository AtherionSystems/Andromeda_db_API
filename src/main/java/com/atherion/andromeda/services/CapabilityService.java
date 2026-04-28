package com.atherion.andromeda.services;

import com.atherion.andromeda.model.Capability;
import com.atherion.andromeda.repositories.CapabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CapabilityService {
    private final CapabilityRepository capabilityRepository;

    public List<Capability> findAll() { return capabilityRepository.findAll(); }
    public List<Capability> findByProjectId(Long projectId) { return capabilityRepository.findByProject_Id(projectId); }
    public Optional<Capability> findById(Long id) { return capabilityRepository.findById(id); }
    public Capability save(Capability capability) { return capabilityRepository.save(capability); }
    public void deleteById(Long id) { capabilityRepository.deleteById(id); }
}
