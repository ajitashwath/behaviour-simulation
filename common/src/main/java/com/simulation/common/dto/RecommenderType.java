package com.simulation.common.dto;

/**
 * Recommender algorithm types.
 */
public enum RecommenderType {
    /**
     * No recommender - use network-based or all-to-all exposure.
     */
    NONE,

    /**
     * Random baseline - shuffle content randomly.
     */
    RANDOM,

    /**
     * Thompson Sampling - Bayesian bandit for engagement optimization.
     * Models real platform algorithms that maximize clicks/engagement.
     */
    THOMPSON_SAMPLING
}
