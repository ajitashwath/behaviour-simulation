package com.simulation.spark.step;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import com.simulation.common.dto.SimulationParams;

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

        private final SimulationParams params;

        public NetworkExposureStep(SimulationParams params) {
                this.params = params;
        }

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
                        Dataset<Row> rankedContent,
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
                Dataset<Row> humanNeighborContent;
                if (previousInteractions.isEmpty()) {
                        // Cold start: all humans see a random subset of content
                        humanNeighborContent = humans.crossJoin(contentWithVisibility.sample(0.1, seed))
                                        .withColumn("neighbor_count", lit(0));
                } else {
                        Dataset<Row> fromNetwork = humans
                                        .join(neighborActivity,
                                                        humans.col("humanId").equalTo(neighborActivity.col("humanId")),
                                                        "inner")
                                        .join(contentWithVisibility,
                                                        neighborActivity.col("contentId")
                                                                        .equalTo(contentWithVisibility
                                                                                        .col("contentId")),
                                                        "inner")
                                        .select(
                                                        humans.col("humanId"),
                                                        humans.col("attentionSpan"),
                                                        humans.col("addictionCoeff"),
                                                        humans.col("mood"),
                                                        contentWithVisibility.col("contentId"),
                                                        contentWithVisibility.col("visibility"),
                                                        contentWithVisibility.col("emotionType"),
                                                        contentWithVisibility.col("intensity"),
                                                        neighborActivity.col("neighbor_count"));

                        // Add algorithmic exploration: mix in some content regardless of neighbors
                        Dataset<Row> expHumans = humans.sample(0.1, seed + 1);
                        Dataset<Row> expContent = contentWithVisibility.sample(0.05, seed + 2);
                        Dataset<Row> explorationContent = expHumans // 10% of users exploring
                                        .crossJoin(expContent)
                                        .withColumn("neighbor_count", lit(0))
                                        .select(
                                                        col("humanId"),
                                                        col("attentionSpan"),
                                                        col("addictionCoeff"),
                                                        col("mood"),
                                                        col("contentId"),
                                                        col("visibility"),
                                                        col("emotionType"),
                                                        col("intensity"),
                                                        col("neighbor_count"));

                        humanNeighborContent = fromNetwork.unionByName(explorationContent).dropDuplicates("humanId",
                                        "contentId");
                }

                // Step 4: Calculate exposure probability
                // Formula: P(expose) = attention * addiction * visibility * social_proof *
                // (optional) recommender_score

                Dataset<Row> withExposureProb;

                if (rankedContent != null) {
                        // Join with recommender scores using sequence of columns to avoid duplicate
                        // columns
                        Dataset<Row> withScores = humanNeighborContent
                                        .join(rankedContent.select(col("humanId"), col("contentId"), col("score")),
                                                        scala.collection.JavaConverters
                                                                        .asScalaBuffer(java.util.Arrays
                                                                                        .asList("humanId", "contentId"))
                                                                        .toSeq(),
                                                        "left");

                        withExposureProb = withScores
                                        .withColumn("social_proof",
                                                        // More neighbors consuming → exponentially higher probability
                                                        lit(1.0).minus(pow(lit(params.getSocialProofBase()), col("neighbor_count"))))
                                        .withColumn("recommender_score",
                                                        when(col("score").isNotNull(), col("score"))
                                                                        .otherwise(lit(0.0)))
                                        .withColumn("exposureProb",
                                                        col("attentionSpan")
                                                                        .multiply(col("addictionCoeff"))
                                                                        .multiply(col("visibility"))
                                                                        // Blend social proof and algorithmic
                                                                        // recommendation (e.g. 50/50 mix when both
                                                                        // present)
                                                                        // Crucially, if social_proof is 0
                                                                        // (exploration), recommender_score can still
                                                                        // drive exposure!
                                                                        .multiply(col("social_proof").multiply(lit(0.5))
                                                                                        .plus(col("recommender_score")
                                                                                                        .multiply(lit(0.5)))));
                } else {
                        // Default logic without recommender
                        withExposureProb = humanNeighborContent
                                        .withColumn("social_proof",
                                                        lit(1.0).minus(pow(lit(params.getSocialProofBase()), col("neighbor_count"))))
                                        .withColumn("exposureProb",
                                                        col("attentionSpan")
                                                                        .multiply(col("addictionCoeff"))
                                                                        .multiply(col("visibility"))
                                                                        .multiply(col("social_proof")));
                }

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
