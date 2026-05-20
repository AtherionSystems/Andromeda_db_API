package com.atherion.andromeda.dto.dashboard;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class HoursPerUserKPI {
    private String     sprintName;
    private String     userName;
    private BigDecimal actualHours;
    private BigDecimal estimatedHours;
}
