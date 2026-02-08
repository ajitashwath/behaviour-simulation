package com.simulation.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Records a single human-content interaction.
 * 
 * Generated during simulation steps when humans are exposed to content.
 * Used for computing metrics and tracing spread patterns.
 * 
 * Schema Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Interaction implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int SCHEMA_VERSION = 1;

    /**
     * The human who was exposed to content.
     */
    private long humanId;

    /**
     * The content the human was exposed to.
     */
    private long contentId;

    /**
     * Simulation step when this interaction occurred.
     */
    private long timeStep;

    /**
     * Whether the human reacted to the content.
     * 
     * Reactions amplify content spread and affect the human's mood.
     * Non-reactions still count as exposure but have weaker effects.
     */
    private boolean reacted;

    /**
     * Change in the human's emotional state from this interaction.
     * 
     * Positive values indicate shift toward JOY.
     * Negative values indicate shift toward RAGE.
     * 
     * Range: [-1, 1]
     */
    private double emotionDelta;

    /**
     * The type of emotion the content carried.
     * Denormalized for easier aggregation in Spark.
     */
    private EmotionType contentEmotionType;
}
