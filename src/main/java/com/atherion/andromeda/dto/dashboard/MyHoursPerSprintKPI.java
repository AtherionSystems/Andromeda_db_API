package com.atherion.andromeda.dto.dashboard;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MyHoursPerSprintKPI {
    private String     sprintName;
    private BigDecimal actualHours;
    private BigDecimal estimatedHours;
}
