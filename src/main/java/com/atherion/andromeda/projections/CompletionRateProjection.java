package com.atherion.andromeda.projections;

public interface CompletionRateProjection {
    String getSprintName();
    Long   getTotalStories();
    Long   getCompletedStories();
}
