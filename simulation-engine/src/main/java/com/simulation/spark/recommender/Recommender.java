package com.simulation.spark.recommender;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.io.Serializable;

/**
 * Interface for content recommender systems.
 * 
 * Recommenders decide which content to show humans based on:
 * - Past engagement patterns
 * - Content characteristics
 * - Strategic objectives (engagement maximization, diversity, etc.)
 */
public interface Recommender extends Serializable {

    /**
     * Rank content for exposure to humans.
     * 
     * @param humans       Current human states
     * @param content      Available content pool
     * @param interactions Historical interactions (for learning)
     * @param step         Current simulation step
     * @return Scored content-human pairs (humanId, contentId, score)
     */
    Dataset<Row> rankContent(
            Dataset<Row> humans,
            Dataset<Row> content,
            Dataset<Row> interactions,
            long step);

    /**
     * Get recommender name for experimental labeling.
     */
    String getName();
}
