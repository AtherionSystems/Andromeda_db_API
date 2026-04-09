// UserTypeRepository.java
package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTypeRepository extends JpaRepository<UserType, Long> {}