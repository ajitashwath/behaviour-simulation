package com.simulation.common.model;

/**
 * Type of emotional content in the simulation.
 * 
 * Content spreads differently based on its emotion type,
 * and affects human mood differently during exposure.
 */
public enum EmotionType {

    POSITIVE("Joy-inducing content"),
    NEUTRAL("Informational or neutral content"),
    NEGATIVE("Rage-inducing content");

    private final String description;

    EmotionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Map from EmotionType to the Mood it tends to induce.
     */
    public Mood toMood() {
        return switch (this) {
            case POSITIVE -> Mood.JOY;
            case NEUTRAL -> Mood.NEUTRAL;
            case NEGATIVE -> Mood.RAGE;
        };
    }

    /**
     * Map from Mood to corresponding EmotionType.
     */
    public static EmotionType fromMood(Mood mood) {
        return switch (mood) {
            case JOY -> POSITIVE;
            case NEUTRAL -> NEUTRAL;
            case RAGE -> NEGATIVE;
        };
    }
}
