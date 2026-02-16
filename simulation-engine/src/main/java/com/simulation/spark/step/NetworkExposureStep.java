package com.simulation.spark.step;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.io.Serializable;

import static org.apache.spark.sql.functions.*;

/**
 * Network-Based Exposure Step
 * 
 * Computes which content each human is exposed to based on:
 * 1. Network structure (humans see what their neighbors consumed)
 * 2. Content visibility (exponential age decay)
 * 3. Individual traits (attention, addiction)
 * 4. Social proof (more neighbors consuming → higher exposure probability)
 * 
 * This replaces the unrealistic all-to-all exposure model with
 * network-embedded diffusion suitable for research.
 */
public class NetworkExposureStep implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Compute exposures using network structure.
         * 
         * @param humans               Current human states (humanId, mood, attention,
         *                             addiction, fatigue)
         * @param content              Available content (contentId, emotionType,
         *                             intensity, createdAtStep, halfLife)
         * @param networkEdges         Network edges (source, target, weight)
         * @param previousInteractions Interactions from last step (humanId, contentId)
         * @param currentStep          Current simulation step
         * @param seed                 Random seed for reproducibility
         * @return Exposures (humanId, contentId, emotionType, intensity, currentMood,
         *         attentionSpan)
         */
        public Dataset<Row> compute(
                        Dataset<Row> humans,
                        Dataset<Row> content,
                        Dataset<Row> networkEdges,
                        Dataset<Row> previousInteractions,
                        long currentStep,
                        long seed) {

                // Step 1: Compute which content each human's neighbors consumed last step
                Dataset<Row> neighborActivity = computeNeighborActivity(networkEdges, previousInteractions);

                // Step 2: Content visibility based on age decay
                Dataset<Row> contentWithVisibility = content
                                .withColumn("age", lit(currentStep).minus(col("createdAtStep")))
                                .withColumn("visibility",
                                                pow(lit(2.0), col("age").multiply(lit(-1.0)).divide(col("halfLife"))));

                // Step 3: Join humans with content their neighbors consumed
                // Left join ensures all humans participate even if neighbors consumed nothing
                Dataset<Row> humanNeighborContent = humans
                                .join(neighborActivity,
                                                humans.col("humanId").equalTo(neighborActivity.col("humanId")),
                                                "left")
                                .join(contentWithVisibility,
                                                neighborActivity.col("contentId")
                                                                .equalTo(contentWithVisibility.col("contentId")),
                                                "left")
                                .filter(col("contentId").isNotNull()); // Remove humans with no neighbor activity

                // Step 4: Calculate exposure probability
                // Formula: P(expose) = attention * addiction * visibility * social_proof
                Dataset<Row> withExposureProb = humanNeighborContent
                                .withColumn("social_proof",
                                                // More neighbors consuming → exponentially higher probability
                                                // Formula: 1 - 0.5^neighbor_count
                                                // 1 neighbor = 0.5, 2 neighbors = 0.75, 3 neighbors = 0.875, etc.
                                                lit(1.0).minus(pow(lit(0.5), col("neighbor_count"))))
                                .withColumn("exposureProb",
                                                col("attentionSpan")
                                                                .multiply(col("addictionCoeff"))
                                                                .multiply(col("visibility"))
                                                                .multiply(col("social_proof")));

                // Step 5: Stochastic exposure decision
                // Each human-content pair is exposed with computed probability
                return withExposureProb
                                .withColumn("exposureRoll", rand(seed))
                                .withColumn("exposed", col("exposureRoll").lt(col("exposureProb")))
                                .filter(col("exposed"))
                                .select(
                                                col("humanId"),
                                                col("contentId"),
                                                col("emotionType"),
                                                col("intensity"),
                                                col("mood").as("currentMood"),
                                                col("attentionSpan"));
        }

        /**
         * Aggregate which content each human's neighbors consumed last step.
         * 
         * Algorithm:
         * 1. For each edge (human → neighbor)
         * 2. Join with neighbor's interactions
         * 3. Group by (human, content) to count how many neighbors consumed each
         * content
         * 
         * @return (humanId, contentId, neighbor_count)
         */
        private Dataset<Row> computeNeighborActivity(
                        Dataset<Row> networkEdges,
                        Dataset<Row> previousInteractions) {

                // Join edges with interactions to get what neighbors consumed
                // networkEdges: (source=human, target=neighbor)
                // previousInteractions: (humanId=neighbor, contentId)
                return networkEdges
                                .join(previousInteractions,
                                                networkEdges.col("target").equalTo(previousInteractions.col("humanId")),
                                                "inner")
                                .groupBy(
                                                networkEdges.col("source").as("humanId"),
                                                col("contentId"))
                                .agg(count("*").as("neighbor_count"));
        }
}
