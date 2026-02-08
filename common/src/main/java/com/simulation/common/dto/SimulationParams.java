package com.simulation.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration parameters for a simulation run.
 * 
 * Passed via REST API when creating experiments.
 * All parameters have sensible defaults documented below.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimulationParams {

    /**
     * Random seed for reproducibility.
     * Same seed + same params = identical results.
     * 
     * Default: 42 (for reproducible demos)
     */
    @Builder.Default
    private long seed = 42L;

    /**
     * Number of humans in the simulation.
     * 
     * Range: 100 to 1,000,000
     * Default: 10,000
     */
    @Positive
    @Min(100)
    @Max(1_000_000)
    @Builder.Default
    private int populationSize = 10_000;

    /**
     * Number of content items active in the simulation.
     * 
     * Range: 10 to 10,000
     * Default: 100
     */
    @Positive
    @Min(10)
    @Max(10_000)
    @Builder.Default
    private int contentCount = 100;

    // === Spread Multipliers ===
    // These control how fast each emotion spreads through contagion.
    // Values > 1.0 mean faster spread, < 1.0 mean slower spread.

    /**
     * Multiplier for rage spread rate.
     * 
     * At 1.1, rage spreads 10% faster than baseline.
     * This is the key parameter for the required experiment.
     * 
     * Range: 0.5 to 2.0
     * Default: 1.0 (baseline)
     */
    @Min(0)
    @Max(2)
    @Builder.Default
    private double rageSpreadMultiplier = 1.0;

    /**
     * Multiplier for joy spread rate.
     * 
     * Range: 0.5 to 2.0
     * Default: 1.0 (baseline)
     */
    @Min(0)
    @Max(2)
    @Builder.Default
    private double joySpreadMultiplier = 1.0;

    // === Decay and Accumulation Rates ===

    /**
     * Rate at which attention decays each step.
     * 
     * Formula: new_attention = attention * (1 - decayRate) + recovery
     * 
     * Range: 0.0 to 0.5
     * Default: 0.05 (5% decay per step)
     */
    @Builder.Default
    private double attentionDecayRate = 0.05;

    /**
     * Base attention recovery per step (when not interacting).
     * 
     * Range: 0.0 to 0.1
     * Default: 0.02
     */
    @Builder.Default
    private double attentionRecoveryRate = 0.02;

    /**
     * Rate at which fatigue accumulates per interaction.
     * 
     * Range: 0.0 to 0.1
     * Default: 0.02 (2% increase per interaction)
     */
    @Builder.Default
    private double fatigueAccumulationRate = 0.02;

    /**
     * Natural fatigue recovery when not interacting.
     * 
     * Range: 0.0 to 0.05
     * Default: 0.01
     */
    @Builder.Default
    private double fatigueRecoveryRate = 0.01;

    // === Content Parameters ===

    /**
     * Average half-life for content visibility.
     * 
     * Range: 1.0 to 100.0
     * Default: 10.0 steps
     */
    @Min(1)
    @Max(100)
    @Builder.Default
    private double contentHalfLife = 10.0;

    /**
     * Initial fraction of population exposed to each content.
     * 
     * Range: 0.01 to 0.5
     * Default: 0.1 (10% initial exposure)
     */
    @Builder.Default
    private double initialExposureRate = 0.1;

    // === Population Initialization ===

    /**
     * Initial mood distribution.
     * Format: [joyFraction, neutralFraction]
     * rageFraction = 1 - joyFraction - neutralFraction
     * 
     * Default: [0.2, 0.6] = 20% joy, 60% neutral, 20% rage
     */
    @Builder.Default
    private double[] initialMoodDistribution = new double[] { 0.2, 0.6 };

    /**
     * Create parameters for the required rage-vs-joy experiment.
     * 
     * Rage spreads 10% faster than joy.
     */
    public static SimulationParams rageExperiment() {
        return SimulationParams.builder()
                .seed(42L)
                .populationSize(10_000)
                .contentCount(100)
                .rageSpreadMultiplier(1.1) // Key: 10% faster rage
                .joySpreadMultiplier(1.0) // Baseline joy
                .build();
    }

    /**
     * Create parameters for baseline (control) experiment.
     */
    public static SimulationParams baseline() {
        return SimulationParams.builder()
                .seed(42L)
                .populationSize(10_000)
                .contentCount(100)
                .rageSpreadMultiplier(1.0)
                .joySpreadMultiplier(1.0)
                .build();
    }
}
