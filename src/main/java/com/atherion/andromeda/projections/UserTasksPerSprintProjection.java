package com.atherion.andromeda.projections;

public interface UserTasksPerSprintProjection {
    String getSprintName();
    String getUserName();
    Long   getTasksCompleted();
}
