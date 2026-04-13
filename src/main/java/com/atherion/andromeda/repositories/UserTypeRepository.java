// UserTypeRepository.java
package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserTypeRepository extends JpaRepository<UserType, Long> {
    @Query("SELECT ut.id FROM UserType ut WHERE ut.userType = :userType")
    Optional<Long> findIdByUserType(String userType);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserType ut WHERE ut.id = :id")
    void deleteByIdJpql(Long id);
}