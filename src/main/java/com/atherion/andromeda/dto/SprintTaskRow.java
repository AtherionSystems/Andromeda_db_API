package com.atherion.andromeda.dto;

import java.math.BigDecimal;

/**
 * Spring Data native-query projection for the sprint task board.
 * Alias names in the SQL query must match these getter names exactly
 * (case-insensitive after stripping "get").
 */
public interface SprintTaskRow {
    Long       getId();
    String     getTitle();
    String     getStatus();
    String     getPriority();
    BigDecimal getEstimatedHours();
    BigDecimal getActualHours();
    String     getAssignees();
    String     getSprintName();
}
