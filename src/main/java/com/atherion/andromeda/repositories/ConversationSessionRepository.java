package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.ConversationSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationSessionRepository extends JpaRepository<ConversationSessionEntity, Long> {
}
