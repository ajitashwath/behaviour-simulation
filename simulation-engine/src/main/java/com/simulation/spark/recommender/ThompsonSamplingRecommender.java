package com.simulation.spark.recommender;

import lombok.RequiredArgsConstructor;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import static org.apache.spark.sql.functions.*;

/**
 * Thompson Sampling Recommender
 * 
 * Uses Bayesian multi-armed bandit to maximize engagement.
 * Each content item is a "arm" with Beta(α, β) distribution.
 * 
 * Algorithm:
 * 1. For each content, maintain Beta(successes+1, failures+1)
 * 2. Sample from each Beta distribution
 * 3. Rank content by sampled values
 * 4. Update distributions based on engagement feedback
 * 
 * Research Relevance:
 * - Models real platform algorithms (engagement optimization)
 * - Creates feedback loop: RAGE content → high engagement → more exposure
 * - Tests hypothesis: "RL recommenders amplify polarization"
 * 
 * Reference: Chapelle & Li (2011), "An Empirical Evaluation of Thompson
 * Sampling"
 */
@RequiredArgsConstructor
public class ThompsonSamplingRecommender implements Recommender {

    private static final long serialVersionUID = 1L;

    private final double explorationRate; // Smoothing parameter (typical: 0.1-1.0)

    @Override
    public Dataset<Row> rankContent(
            Dataset<Row> humans,
            Dataset<Row> content,
            Dataset<Row> interactions,
            long step) {

        // Step 1: Compute engagement statistics per content
        Dataset<Row> contentStats = computeEngagementStats(content, interactions);

        // Step 2: Sample from Beta distributions
        long seed = step + 12345L;
        Dataset<Row> sampledScores = contentStats
                .withColumn("alpha", col("successes").plus(lit(1.0)))
                .withColumn("beta", col("failures").plus(lit(1.0)))
                // Thompson Sampling: sample from Beta(α, β)
                // Approximation: use mean + noise for computational efficiency
                .withColumn("mean_reward", col("alpha").divide(col("alpha").plus(col("beta"))))
                .withColumn("exploration_bonus",
                        sqrt(col("alpha").plus(col("beta"))).multiply(lit(explorationRate)))
                .withColumn("thompson_sample",
                        col("mean_reward").plus(
                                col("exploration_bonus").multiply(randn(seed))))
                .select(
                        col("contentId"),
                        col("emotionType"),
                        col("thompson_sample").as("score"),
                        col("successes"),
                        col("failures"));

        // Step 3: Cross-join with humans and rank
        return humans
                .select(col("humanId"))
                .crossJoin(sampledScores)
                .select(
                        col("humanId"),
                        col("contentId"),
                        col("emotionType"),
                        col("score"));
    }

    /**
     * Compute engagement successes and failures for each content.
     */
    private Dataset<Row> computeEngagementStats(Dataset<Row> content, Dataset<Row> interactions) {
        if (interactions.isEmpty()) {
            // Cold start: all content has uniform prior
            return content.withColumn("successes", lit(0.0))
                    .withColumn("failures", lit(0.0));
        }

        // Aggregate interactions: engagement = click/like/share
        // For now, any interaction = success (simplified)
        Dataset<Row> stats = interactions
                .groupBy("contentId")
                .agg(count("*").as("successes"))
                .withColumn("failures", lit(0.0)); // Simplified: no explicit failures tracked

        // Left join to include content with no interactions
        return content
                .join(stats, content.col("contentId").equalTo(stats.col("contentId")), "left")
                .select(
                        content.col("contentId"),
                        content.col("emotionType"),
                        coalesce(stats.col("successes"), lit(0.0)).as("successes"),
                        coalesce(col("failures"), lit(0.0)).as("failures"));
    }

    @Override
    public String getName() {
        return String.format("ThompsonSampling_eps%.2f", explorationRate);
    }
}
