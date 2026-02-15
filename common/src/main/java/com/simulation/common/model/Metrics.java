package com.simulation.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * Metrics snapshot for a single simulation step.
 * Extended with polarization measures for RL experiments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Metrics implements Serializable {

    private static final long serialVersionUID = 1L;

    // Basic state
    private long timeStep;
    private Map<String, Long> moodDistribution; // JOY, NEUTRAL, RAGE counts

    // Interaction metrics
    private long totalInteractions;
    private long positiveInteractions;
    private long negativeInteractions;

    // Engagement metrics (for RL recommenders)
    private double avgEngagement; // Mean interactions per human
    private double engagementGini; // Inequality in engagement distribution

    // Polarization metrics
    private double polarizationIndex; // |RAGE - JOY| / population
    private double extremismRate; // (RAGE + JOY) / population
    private double fragmentationIndex; // 1 - NEUTRAL / population

    // Convenience getters
    public Long getJoyCount() {
        return moodDistribution.getOrDefault("JOY", 0L);
    }

    public Long getNeutralCount() {
        return moodDistribution.getOrDefault("NEUTRAL", 0L);
    }

    public Long getRageCount() {
        return moodDistribution.getOrDefault("RAGE", 0L);
    }
}
