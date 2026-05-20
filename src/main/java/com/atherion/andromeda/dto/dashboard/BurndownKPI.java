package com.atherion.andromeda.dto.dashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BurndownKPI {
    private String sprintName;
    private long   totalStories;
    private long   completedStories;
    private long   remainingStories;
    private long   totalTasks;
    private long   completedTasks;
    private long   remainingTasks;
    private long   totalPoints;
    private long   completedPoints;
    private long   remainingPoints;
}
