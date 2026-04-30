package com.atherion.andromeda.dto.dashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserTasksPerSprintKPI {
    private String sprintName;
    private String userName;
    private long   tasksCompleted;
}
