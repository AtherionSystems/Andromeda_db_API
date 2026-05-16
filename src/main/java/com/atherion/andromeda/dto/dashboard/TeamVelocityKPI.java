package com.atherion.andromeda.dto.dashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeamVelocityKPI {
    private String sprintName;
    private long   pointsCompleted;
    private long   pointsPlanned;
}
