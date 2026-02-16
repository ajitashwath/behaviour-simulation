package com.simulation.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Continuous 2D affect space representation.
 * Based on Russell's circumplex model of affect.
 * 
 * Emotions are represented as combinations of:
 * - Valence: negative to positive (displeasure to pleasure)
 * - Arousal: calm to excited (low to high activation)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmotionalState implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Valence dimension: [-1, 1]
     * -1 = maximally negative (displeasure)
     * 0 = neutral
     * +1 = maximally positive (pleasure)
     */
    private double valence;

    /**
     * Arousal dimension: [-1, 1]
     * -1 = maximally calm (deactivated)
     * 0 = neutral activation
     * +1 = maximally excited (activated)
     */
    private double arousal;

    /**
     * Map continuous affect to discrete mood for backward compatibility.
     * 
     * Mapping logic:
     * - NEUTRAL: Near origin (low valence and arousal)
     * - JOY: Positive valence (regardless of arousal)
     * - RAGE: Negative valence with high arousal
     * - SADNESS: Negative valence with low arousal (maps to RAGE for compatibility)
     * 
     * @return Discrete mood category
     */
    public Mood getDiscreteMood() {
        // Near origin: neutral
        if (Math.abs(valence) < 0.3 && Math.abs(arousal) < 0.3) {
            return Mood.NEUTRAL;
        }

        // Positive valence: joy
        if (valence > 0.3) {
            return Mood.JOY;
        }

        // Negative valence: rage (regardless of arousal for backward compatibility)
        if (valence < -0.3) {
            return Mood.RAGE;
        }

        return Mood.NEUTRAL;
    }

    /**
     * Initialize from discrete mood (for backward compatibility).
     * 
     * Default mappings:
     * - JOY → valence=0.7, arousal=0.5 (pleasant, moderately activated)
     * - RAGE → valence=-0.7, arousal=0.8 (unpleasant, highly activated)
     * - NEUTRAL → valence=0.0, arousal=0.0 (neutral)
     * 
     * @param mood Discrete mood category
     * @return Corresponding emotional state
     */
    public static EmotionalState fromDiscrete(Mood mood) {
        switch (mood) {
            case JOY:
                return new EmotionalState(0.7, 0.5);
            case RAGE:
                return new EmotionalState(-0.7, 0.8);
            case NEUTRAL:
            default:
                return new EmotionalState(0.0, 0.0);
        }
    }

    /**
     * Compute Euclidean distance to another emotional state in affect space.
     * 
     * Useful for measuring emotional similarity and contagion effects.
     * 
     * @param other Another emotional state
     * @return Distance in affect space [0, sqrt(8) ≈ 2.83]
     */
    public double distanceTo(EmotionalState other) {
        double dv = this.valence - other.valence;
        double da = this.arousal - other.arousal;
        return Math.sqrt(dv * dv + da * da);
    }

    /**
     * Clip values to valid range [-1, 1].
     * 
     * Call this after any arithmetic operations that might push
     * values out of bounds.
     */
    public void clipToRange() {
        this.valence = Math.max(-1.0, Math.min(1.0, this.valence));
        this.arousal = Math.max(-1.0, Math.min(1.0, this.arousal));
    }

    /**
     * Create emotional state with clipping applied.
     * 
     * @param valence Valence value (will be clipped to [-1, 1])
     * @param arousal Arousal value (will be clipped to [-1, 1])
     * @return Clipped emotional state
     */
    public static EmotionalState createClipped(double valence, double arousal) {
        EmotionalState state = new EmotionalState(valence, arousal);
        state.clipToRange();
        return state;
    }

    /**
     * Interpolate between two emotional states.
     * 
     * @param other Target state
     * @param alpha Interpolation factor [0, 1]
     * @return Interpolated state (this * (1-alpha) + other * alpha)
     */
    public EmotionalState interpolate(EmotionalState other, double alpha) {
        double newValence = this.valence * (1 - alpha) + other.valence * alpha;
        double newArousal = this.arousal * (1 - alpha) + other.arousal * alpha;
        return createClipped(newValence, newArousal);
    }

    @Override
    public String toString() {
        return String.format("EmotionalState(valence=%.2f, arousal=%.2f, mood=%s)",
                valence, arousal, getDiscreteMood());
    }
}
