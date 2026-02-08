package com.simulation.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Represents content in the simulation that humans interact with.
 * 
 * Content has an emotional type that influences how it spreads
 * and what mood changes it induces in exposed humans.
 * 
 * Schema Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Content implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int SCHEMA_VERSION = 1;

    /**
     * Unique identifier for this content.
     */
    private long contentId;

    /**
     * Emotional category of the content.
     * Determines spread multiplier and mood influence.
     */
    private EmotionType emotionType;

    /**
     * Emotional impact strength. Range: [0, 1]
     * 
     * Higher intensity means stronger mood influence on exposed humans.
     * Also affects viral spread probability.
     * 
     * Example: 0.9 = highly impactful content, 0.2 = mild content.
     */
    private double intensity;

    /**
     * Decay rate in time steps.
     * 
     * Number of steps for content visibility to halve.
     * Lower values mean faster decay (more ephemeral content).
     * 
     * Example: 5.0 = content loses half its reach every 5 steps.
     */
    private double halfLife;

    /**
     * When the content was introduced to the simulation.
     * Used for age-based decay calculations.
     */
    private long createdAtStep;

    /**
     * Calculate content visibility at a given time step.
     * Uses exponential decay based on half-life.
     * 
     * Formula: base * 2^(-(currentStep - createdAtStep) / halfLife)
     * 
     * @param currentStep the current simulation step
     * @return visibility factor between 0 and 1
     */
    public double getVisibilityAt(long currentStep) {
        if (currentStep < createdAtStep) {
            return 0.0; // Content doesn't exist yet
        }

        double age = currentStep - createdAtStep;
        // Exponential decay: visibility = 2^(-age/halfLife)
        return Math.pow(2.0, -age / halfLife);
    }

    /**
     * Check if content is effectively "dead" (visibility too low).
     * Threshold at 1% visibility.
     */
    public boolean isExpired(long currentStep) {
        return getVisibilityAt(currentStep) < 0.01;
    }

    /**
     * Create sample content for testing.
     */
    public static Content createSample(long contentId, EmotionType type) {
        return Content.builder()
                .contentId(contentId)
                .emotionType(type)
                .intensity(0.5)
                .halfLife(10.0)
                .createdAtStep(0)
                .build();
    }
}
