package com.simulation.spark.step;

import com.simulation.common.dto.SimulationParams;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import static org.apache.spark.sql.functions.*;

/**
 * Update emotions using continuous affect space dynamics.
 * 
 * Implements Russell's circumplex model of affect with:
 * - Content influence (direct emotional push from exposed content)
 * - Social contagion (emotional pull toward network neighbors)
 * - Baseline regression (emotional regulation toward individual trait)
 * - Arousal decay (arousal returns to baseline faster than valence)
 * 
 * Replaces MoodContagionStep when useContinuousEmotions=true.
 */
public class ContinuousMoodContagionStep {

    private final SimulationParams params;

    public ContinuousMoodContagionStep(SimulationParams params) {
        this.params = params;
    }

    /**
     * Apply continuous emotion dynamics.
     * 
     * @param humans       Current human state
     * @param reactions    Recent content reactions
     * @param networkEdges Social network structure
     * @return Updated human state with new emotional values
     */
    public Dataset<Row> apply(Dataset<Row> humans,
            Dataset<Row> reactions,
            Dataset<Row> networkEdges) {

        // 1. Compute neighbor average emotions (social contagion)
        Dataset<Row> neighborEmotions = computeNeighborEmotions(humans, networkEdges);

        // 2. Compute content exposure effects
        Dataset<Row> contentEffects = computeContentEffects(reactions);

        // 3. Update emotions using weighted combination
        Dataset<Row> updated = updateEmotions(humans, neighborEmotions, contentEffects);

        return updated;
    }

    /**
     * Compute average emotional state of network neighbors.
     */
    private Dataset<Row> computeNeighborEmotions(Dataset<Row> humans,
            Dataset<Row> networkEdges) {
        return humans
                .join(networkEdges, humans.col("humanId").equalTo(networkEdges.col("source")))
                .join(humans.as("neighbor"), networkEdges.col("target").equalTo(col("neighbor.humanId")))
                .groupBy("source")
                .agg(
                        avg("neighbor.valence").as("neighbor_valence"),
                        avg("neighbor.arousal").as("neighbor_arousal"))
                .withColumnRenamed("source", "humanId");
    }

    /**
     * Compute weighted average of content emotional affect.
     * Weight by intensity to give stronger weight to more impactful content.
     */
    private Dataset<Row> computeContentEffects(Dataset<Row> reactions) {
        return reactions
                .groupBy("humanId")
                .agg(
                        // Weighted average: sum(valence * intensity) / sum(intensity)
                        sum(col("contentValence").multiply(col("intensity")))
                                .divide(sum(col("intensity")))
                                .as("content_valence"),
                        sum(col("contentArousal").multiply(col("intensity")))
                                .divide(sum(col("intensity")))
                                .as("content_arousal"),
                        // Also track total exposure intensity
                        sum(col("intensity")).as("total_intensity"));
    }

    /**
     * Update emotional state using weighted combination of influences.
     * 
     * Formula:
     * new_emotion = persistence * current
     * + content_weight * content_affect
     * + social_weight * neighbor_affect
     * + regression_weight * baseline
     * 
     * Weights are normalized to sum to 1.0 for stability.
     */
    private Dataset<Row> updateEmotions(Dataset<Row> humans,
            Dataset<Row> neighborEmotions,
            Dataset<Row> contentEffects) {

        // Weight parameters
        double persistenceWeight = 0.70; // Keep 70% of current state
        double contentWeight = params.getContentInfluenceWeight(); // Default: 0.15
        double socialWeight = params.getSocialInfluenceWeight(); // Default: 0.10
        double regressionWeight = params.getEmotionRegressionRate(); // Default: 0.05

        // Join all influences
        Dataset<Row> combined = humans
                .join(neighborEmotions, "humanId", "left")
                .join(contentEffects, "humanId", "left");

        // Update valence
        Dataset<Row> withNewValence = combined
                .withColumn("newValence",
                        // Persistence
                        col("valence").multiply(persistenceWeight)
                                // Content influence (conditional on exposure)
                                .plus(coalesce(col("content_valence"), lit(0.0)).multiply(contentWeight))
                                // Social contagion (conditional on having neighbors)
                                .plus(coalesce(col("neighbor_valence"), lit(0.0)).multiply(socialWeight))
                                // Regression to baseline
                                .plus(coalesce(col("baselineValence"), lit(0.0)).multiply(regressionWeight)));

        // Update arousal (with additional decay)
        Dataset<Row> withNewArousal = withNewValence
                .withColumn("arousalPersistence",
                        lit(persistenceWeight * (1.0 - params.getArousalDecayRate())))
                .withColumn("newArousal",
                        // Persistence (with extra decay)
                        col("arousal").multiply(col("arousalPersistence"))
                                // Content influence
                                .plus(coalesce(col("content_arousal"), lit(0.0)).multiply(contentWeight))
                                // Social contagion
                                .plus(coalesce(col("neighbor_arousal"), lit(0.0)).multiply(socialWeight))
                                // Regression to baseline
                                .plus(coalesce(col("baselineArousal"), lit(0.0)).multiply(regressionWeight)))
                .drop("arousalPersistence");

        // Clip to valid range [-1, 1] and update discrete mood
        Dataset<Row> clipped = withNewArousal
                // Clip valence
                .withColumn("valence",
                        when(col("newValence").gt(1.0), 1.0)
                                .when(col("newValence").lt(-1.0), -1.0)
                                .otherwise(col("newValence")))
                // Clip arousal
                .withColumn("arousal",
                        when(col("newArousal").gt(1.0), 1.0)
                                .when(col("newArousal").lt(-1.0), -1.0)
                                .otherwise(col("newArousal")))
                // Update discrete mood for backward compatibility
                .withColumn("mood",
                        when(col("valence").gt(0.3), lit("JOY"))
                                .when(col("valence").lt(-0.3), lit("RAGE"))
                                .otherwise(lit("NEUTRAL")));

        // Clean up temporary columns
        return clipped.drop(
                "newValence", "newArousal",
                "content_valence", "content_arousal", "total_intensity",
                "neighbor_valence", "neighbor_arousal");
    }
}
