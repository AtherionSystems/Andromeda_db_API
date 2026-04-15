// UserService.java
package com.atherion.andromeda.services;

import com.atherion.andromeda.model.User;
import com.atherion.andromeda.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public List<User> findAll() { return userRepository.findAll(); }
    public Optional<User> findById(Long id) { return userRepository.findById(id); }
    public Optional<User> findByUsername(String username) { return userRepository.findByUsername(username); }
    public Optional<User> findByTelegramId(Long telegramId) { return userRepository.findByTelegramId(telegramId); }
    public User save(User user) { return userRepository.save(user); }
    public void deleteById(Long id) { userRepository.deleteById(id); }
    public boolean usernameExists(String username) { return userRepository.existsByUsername(username); }
    public boolean emailExists(String email) { return userRepository.existsByEmail(email); }
    public boolean telegramIdExists(Long telegramId) { return userRepository.existsByTelegramId(telegramId); }
}