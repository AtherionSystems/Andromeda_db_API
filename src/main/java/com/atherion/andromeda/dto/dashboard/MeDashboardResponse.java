package com.atherion.andromeda.dto.dashboard;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MeDashboardResponse {
    private Long                       userId;
    private String                     userName;
    private Long                       projectId;
    private LocalDateTime              generatedAt;
    private List<TaskDistributionKPI>  myTaskDistribution;
    private List<MyHoursPerSprintKPI>  myHoursPerSprint;
    private List<MyTasksPerSprintKPI>  myTasksPerSprint;
}
