package com.simulation.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * Aggregated metrics for a single simulation time step.
 * 
 * Computed by Spark after each step and persisted for analysis.
 * These metrics enable tracking system-level dynamics over time.
 * 
 * Schema Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Metrics implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int SCHEMA_VERSION = 1;

    /**
     * Simulation step these metrics correspond to.
     */
    private long timeStep;

    // === Mood Distribution Metrics ===

    /**
     * Count of humans in each mood state.
     * Keys: "JOY", "NEUTRAL", "RAGE"
     */
    private Map<String, Long> moodDistribution;

    /**
     * Shannon entropy of mood distribution. Range: [0, log(3)]
     * 
     * Formula: -Σ(p * log(p)) where p = fraction in each mood
     * 
     * Higher values indicate more balanced distribution.
     * Lower values indicate mood polarization.
     * 
     * Max entropy (perfectly balanced): ~1.585 (log2(3))
     * Min entropy (all one mood): 0
     */
    private double moodEntropy;

    /**
     * Ratio of the dominant mood to total population. Range: [0.33, 1.0]
     * 
     * Formula: max(mood_counts) / total_population
     * 
     * Values near 0.33 indicate balanced moods.
     * Values near 1.0 indicate extreme polarization.
     */
    private double emotionDominanceRatio;

    // === Spread Dynamics Metrics ===

    /**
     * Rage reproduction rate (R-value). Range: [0, ∞)
     * 
     * Formula: new_rage_cases / current_rage_count
     * 
     * If R > 1: Rage is spreading (epidemic growth)
     * If R < 1: Rage is declining
     * If R ≈ 1: Stable endemic state
     * 
     * Similar to epidemiological R0, but for emotional states.
     */
    private double rageReproductionRate;

    /**
     * Joy reproduction rate for comparison.
     */
    private double joyReproductionRate;

    // === Attention & Engagement Metrics ===

    /**
     * Gini coefficient of attention distribution. Range: [0, 1]
     * 
     * Measures inequality in attention spans across population.
     * 
     * 0 = perfect equality (everyone has same attention)
     * 1 = maximum inequality (one person has all attention)
     * 
     * Increasing Gini suggests attention concentration.
     */
    private double attentionGini;

    /**
     * Average attention span across population. Range: [0, 1]
     */
    private double averageAttention;

    // === Fatigue Metrics ===

    /**
     * Average fatigue across population. Range: [0, 1]
     */
    private double averageFatigue;

    /**
     * 95th percentile fatigue level. Range: [0, 1]
     * 
     * Tracks the "fatigue collapse threshold" - when this
     * approaches 1.0, the system is near mass disengagement.
     */
    private double fatigueP95;

    /**
     * Count of humans who have "collapsed" (fatigue >= 0.95).
     */
    private long collapsedCount;

    // === Content Metrics ===

    /**
     * Average content survival time by emotion type.
     * Keys: "POSITIVE", "NEUTRAL", "NEGATIVE"
     * Values: average steps before content becomes expired.
     */
    private Map<String, Double> contentSurvivalByEmotion;

    /**
     * Total interactions this step.
     */
    private long totalInteractions;

    /**
     * Total reactions (interaction where human reacted).
     */
    private long totalReactions;

    /**
     * Reaction rate: reactions / interactions.
     */
    private double reactionRate;
}
