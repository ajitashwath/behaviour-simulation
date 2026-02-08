package com.simulation.common.model;

/**
 * Emotional state of a simulated human.
 * 
 * Each mood has a numeric valence for calculations:
 * - JOY: positive (+1.0)
 * - NEUTRAL: baseline (0.0)  
 * - RAGE: negative (-1.0)
 */
public enum Mood {
    
    JOY(1.0, "Positive emotional state"),
    NEUTRAL(0.0, "Baseline emotional state"),
    RAGE(-1.0, "Negative emotional state");

    private final double valence;
    private final String description;

    Mood(double valence, String description) {
        this.valence = valence;
        this.description = description;
    }

    /**
     * Numeric representation of emotional valence.
     * Used in contagion calculations.
     */
    public double getValence() {
        return valence;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Convert a numeric valence back to the nearest Mood.
     * Useful after averaging or contagion calculations.
     * 
     * @param valence numeric value (typically between -1 and 1)
     * @return closest Mood enum value
     */
    public static Mood fromValence(double valence) {
        if (valence > 0.5) {
            return JOY;
        } else if (valence < -0.5) {
            return RAGE;
        } else {
            return NEUTRAL;
        }
    }
}
