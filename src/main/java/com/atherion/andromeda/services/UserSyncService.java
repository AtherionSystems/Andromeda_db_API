package com.atherion.andromeda.services;

import com.atherion.andromeda.model.User;
import com.atherion.andromeda.model.UserType;
import com.atherion.andromeda.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSyncService {

    private final UserRepository userRepository;
    private final EntityManager entityManager;

    private static final Long DEFAULT_USER_TYPE_ID = 212L;

    @Transactional
    public User syncOAuthUser(String iamSub, String displayName) {
        // iamSub en OCI es el email (sub = "a0157122@tec.mx")
        // Primero busca por iamSub por si ya fue sincronizado antes
        return userRepository.findByIamSub(iamSub)
                .orElseGet(() -> userRepository.findByEmail(iamSub)
                        .map(existing -> {
                            // Usuario legacy encontrado por email: vincula el iamSub
                            existing.setIamSub(iamSub);
                            log.info("Vinculando usuario legacy {} con IAM sub", iamSub);
                            return userRepository.save(existing);
                        })
                        .orElseGet(() -> {
                            // Usuario nuevo: crear registro
                            User newUser = new User();
                            newUser.setIamSub(iamSub);
                            newUser.setEmail(iamSub);
                            newUser.setName(displayName != null ? displayName : iamSub);
                            newUser.setUsername(iamSub);
                            newUser.setPasswordHash("OAUTH");
                            newUser.setUserType(entityManager.getReference(UserType.class, DEFAULT_USER_TYPE_ID));
                            log.info("Creando nuevo usuario OAuth: {}", iamSub);
                            return userRepository.save(newUser);
                        })
                );
    }
}