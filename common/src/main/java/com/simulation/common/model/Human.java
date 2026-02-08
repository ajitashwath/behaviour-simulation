package com.simulation.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Represents a simulated human in the population.
 * 
 * This is the core entity that evolves through simulation steps.
 * All fields are designed to be serializable to Parquet for Spark processing.
 * 
 * Schema Version: 1.0
 */
@Data // Generates getters, setters, toString, equals, hashCode
@Builder(toBuilder = true) // Provides fluent builder pattern with toBuilder()
@NoArgsConstructor // Required for serialization frameworks
@AllArgsConstructor // Used by the builder
public class Human implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Schema version for forward compatibility.
     * Increment when adding/removing fields.
     */
    public static final int SCHEMA_VERSION = 1;

    /**
     * Unique identifier for this human.
     * Must be stable across simulation steps.
     */
    private long humanId;

    /**
     * Current emotional state.
     * Updates each step based on content exposure and contagion.
     */
    private Mood mood;

    /**
     * Capacity for consuming content. Range: [0, 1]
     * 
     * Higher values mean the human can process more content per step.
     * Decays over time due to fatigue, recovers when not consuming.
     * 
     * Example: 0.8 means high attention, 0.2 means easily distracted.
     */
    private double attentionSpan;

    /**
     * Tendency to seek engagement. Range: [0, 1]
     * 
     * Higher values increase exposure probability and reaction rates.
     * This is a stable trait (does not change during simulation).
     * 
     * Example: 0.9 means highly addicted to content consumption.
     */
    private double addictionCoeff;

    /**
     * Likelihood of reacting to content. Range: [0, 1]
     * 
     * Modified by fatigue during simulation.
     * Base probability before fatigue adjustment.
     * 
     * Example: 0.5 means 50% chance of reacting when exposed.
     */
    private double reactionProb;

    /**
     * Accumulated exhaustion. Range: [0, 1]
     * 
     * Increases with each interaction.
     * Reduces effective reaction probability.
     * When near 1.0, the human becomes "collapsed" (minimal engagement).
     * 
     * Example: 0.0 = fresh, 1.0 = completely exhausted.
     */
    private double fatigue;

    /**
     * Create a human with default neutral state.
     * Useful for testing and initialization.
     */
    public static Human createDefault(long humanId) {
        return Human.builder()
                .humanId(humanId)
                .mood(Mood.NEUTRAL)
                .attentionSpan(0.7)
                .addictionCoeff(0.5)
                .reactionProb(0.5)
                .fatigue(0.0)
                .build();
    }

    /**
     * Calculate effective reaction probability considering fatigue.
     * 
     * Formula: reactionProb * (1 - fatigue)
     * As fatigue increases, reaction probability decreases.
     */
    public double getEffectiveReactionProb() {
        return reactionProb * (1.0 - fatigue);
    }

    /**
     * Check if this human has "collapsed" (fatigue too high to function).
     */
    public boolean isCollapsed() {
        return fatigue >= 0.95;
    }
}
