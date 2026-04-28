package com.atherion.andromeda.projections;

public interface TeamVelocityProjection {
    String getSprintName();
    Long   getPointsCompleted();
    Long   getPointsPlanned();
}