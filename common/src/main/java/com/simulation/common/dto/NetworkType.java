package com.simulation.common.dto;

/**
 * Types of social network topologies supported by the simulation.
 * 
 * Each topology has different structural properties that affect
 * how emotions spread through the population.
 */
public enum NetworkType {
    /**
     * All-to-all exposure (current implementation).
     * Every human potentially sees all content.
     * 
     * Properties:
     * - Unrealistic for social media
     * - Fastest spread
     * - No network effects
     * - Useful as baseline
     */
    ALL_TO_ALL,

    /**
     * Barabási-Albert scale-free network.
     * Power-law degree distribution with hub nodes.
     * 
     * Properties:
     * - Realistic for Facebook, Twitter, Reddit
     * - P(k) ~ k^(-γ) where γ ≈ 3
     * - Hub nodes act as "superspreaders"
     * - Heterogeneous degree distribution
     * 
     * Reference: Barabási & Albert (1999)
     */
    BARABASI_ALBERT,

    /**
     * Watts-Strogatz small-world network.
     * High clustering + short paths.
     * 
     * Properties:
     * - Models "six degrees of separation"
     * - High local clustering → echo chambers
     * - Short global paths → fast spread
     * - Tunable via rewiring probability
     * 
     * Reference: Watts & Strogatz (1998)
     */
    WATTS_STROGATZ,

    /**
     * Erdős-Rényi random graph.
     * Each edge exists independently with probability p.
     * 
     * Properties:
     * - Null model for network experiments
     * - No structure, pure randomness
     * - Poisson degree distribution
     * - Low clustering
     * 
     * Reference: Erdős & Rényi (1959)
     */
    RANDOM
}
