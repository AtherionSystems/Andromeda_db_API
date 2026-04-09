// UserTypeService.java
package com.atherion.andromeda.services;

import com.atherion.andromeda.model.UserType;
import com.atherion.andromeda.repositories.UserTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserTypeService {
    private final UserTypeRepository userTypeRepository;

    public List<UserType> findAll() { return userTypeRepository.findAll(); }
    public Optional<UserType> findById(Long id) { return userTypeRepository.findById(id); }
    public UserType save(UserType userType) { return userTypeRepository.save(userType); }
    public void deleteById(Long id) { userTypeRepository.deleteById(id); }
}