package com.atherion.andromeda.projections;

import java.math.BigDecimal;

public interface HoursPerUserProjection {
    String     getSprintName();
    String     getUserName();
    BigDecimal getActualHours();
    BigDecimal getEstimatedHours();
}
