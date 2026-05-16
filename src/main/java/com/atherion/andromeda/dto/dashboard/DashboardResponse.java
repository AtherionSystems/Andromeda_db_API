package com.atherion.andromeda.dto.dashboard;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private Long                       projectId;
    private LocalDateTime              generatedAt;
    private List<CompletionRateKPI>    completionRateBySprint;
    private List<TeamVelocityKPI>      teamVelocity;
    private List<TaskDistributionKPI>  taskDistribution;
    private List<UserTasksPerSprintKPI> userTasksPerSprint;
}