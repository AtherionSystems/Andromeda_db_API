package com.atherion.andromeda.projections;

import java.math.BigDecimal;

public interface MyHoursPerSprintProjection {
    String     getSprintName();
    BigDecimal getActualHours();
    BigDecimal getEstimatedHours();
}
