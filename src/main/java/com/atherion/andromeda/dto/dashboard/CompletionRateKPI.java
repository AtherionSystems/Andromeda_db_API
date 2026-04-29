package com.atherion.andromeda.dto.dashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompletionRateKPI {
    private String sprintName;
    private long   totalStories;
    private long   completedStories;
    private double completionRate;
}
