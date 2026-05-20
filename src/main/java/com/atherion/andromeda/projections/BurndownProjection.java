package com.atherion.andromeda.projections;

public interface BurndownProjection {
    String getSprintName();
    Long   getTotalStories();
    Long   getCompletedStories();
    Long   getTotalTasks();
    Long   getCompletedTasks();
    Long   getTotalPoints();
    Long   getCompletedPoints();
}
