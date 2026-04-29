// SprintRepository.java
package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {
    List<Sprint> findByProject_Id(Long projectId);
    List<Sprint> findTop2ByProject_IdOrderByCreatedAtDesc(Long projectId);
}
