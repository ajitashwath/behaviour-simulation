package com.simulation.spark.metrics;

import com.simulation.common.model.Metrics;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.apache.spark.sql.functions.*;

/**
 * Aggregates metrics from simulation state.
 * 
 * Computes all required metrics using Spark SQL aggregations.
 */
public class MetricsAggregator implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Compute all metrics for a time step.
     * 
     * @param humans    current population state
     * @param reactions reactions from this step
     * @param timeStep  current step number
     * @return aggregated metrics
     */
    public Metrics compute(Dataset<Row> humans, Dataset<Row> reactions, long timeStep) {
        long population = humans.count();

        // Mood distribution
        Map<String, Long> moodDist = computeMoodDistribution(humans);

        // Mood entropy
        double entropy = computeMoodEntropy(moodDist, population);

        // Dominance ratio
        double dominance = computeDominanceRatio(moodDist, population);

        // Attention Gini coefficient
        double gini = computeAttentionGini(humans);

        // Basic stats
        Row stats = humans.agg(
                avg("attentionSpan"),
                avg("fatigue"),
                expr("percentile_approx(fatigue, 0.95)")).first();

        double avgAttention = stats.isNullAt(0) ? 0.0 : stats.getDouble(0);
        double avgFatigue = stats.isNullAt(1) ? 0.0 : stats.getDouble(1);
        double fatigueP95 = stats.isNullAt(2) ? 0.0 : stats.getDouble(2);

        // Collapsed count
        long collapsed = humans.filter(col("fatigue").geq(0.95)).count();

        // Reaction stats
        long totalInteractions = 0;
        long totalReactions = 0;
        if (!reactions.isEmpty()) {
            totalInteractions = reactions.count();
            totalReactions = reactions.filter(col("reacted")).count();
        }

        double reactionRate = totalInteractions > 0
                ? (double) totalReactions / totalInteractions
                : 0.0;

        return Metrics.builder()
                .timeStep(timeStep)
                .moodDistribution(moodDist)
                .moodEntropy(entropy)
                .emotionDominanceRatio(dominance)
                .rageReproductionRate(1.0) // TODO: calculate from step-over-step
                .joyReproductionRate(1.0)
                .attentionGini(gini)
                .averageAttention(avgAttention)
                .averageFatigue(avgFatigue)
                .fatigueP95(fatigueP95)
                .collapsedCount(collapsed)
                .totalInteractions(totalInteractions)
                .totalReactions(totalReactions)
                .reactionRate(reactionRate)
                .build();
    }

    private Map<String, Long> computeMoodDistribution(Dataset<Row> humans) {
        Map<String, Long> dist = new HashMap<>();
        dist.put("JOY", 0L);
        dist.put("NEUTRAL", 0L);
        dist.put("RAGE", 0L);

        humans.groupBy("mood")
                .count()
                .collectAsList()
                .forEach(row -> {
                    String mood = row.getString(0);
                    long count = row.getLong(1);
                    dist.put(mood, count);
                });

        return dist;
    }

    /**
     * Compute Shannon entropy of mood distribution.
     * 
     * Formula: H = -Σ(p * log2(p))
     * 
     * Range: [0, log2(3)] ≈ [0, 1.585]
     * - 0 = all one mood (minimum diversity)
     * - log2(3) = perfectly balanced (maximum diversity)
     */
    private double computeMoodEntropy(Map<String, Long> moodDist, long population) {
        if (population == 0)
            return 0.0;

        double entropy = 0.0;
        for (long count : moodDist.values()) {
            if (count > 0) {
                double p = (double) count / population;
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }
        return entropy;
    }

    /**
     * Compute emotion dominance ratio.
     * 
     * Formula: max(mood_counts) / population
     * 
     * Range: [0.33, 1.0]
     * - 0.33 = perfectly balanced
     * - 1.0 = complete polarization
     */
    private double computeDominanceRatio(Map<String, Long> moodDist, long population) {
        if (population == 0)
            return 0.0;

        long maxCount = moodDist.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);

        return (double) maxCount / population;
    }

    /**
     * Compute Gini coefficient for attention distribution.
     * 
     * Uses Spark's built-in functions for efficiency.
     * 
     * Formula: (2 * Σ(i * x[i])) / (n * Σ(x[i])) - (n + 1) / n
     * 
     * Range: [0, 1]
     * - 0 = perfect equality
     * - 1 = maximum inequality
     */
    private double computeAttentionGini(Dataset<Row> humans) {
        // Get sorted attention values with indices
        Dataset<Row> sorted = humans
                .select("attentionSpan")
                .orderBy("attentionSpan")
                .withColumn("idx", monotonically_increasing_id());

        long n = sorted.count();
        if (n == 0)
            return 0.0;

        // Compute required sums
        Row sums = sorted.agg(
                sum("attentionSpan"),
                sum(col("idx").multiply(col("attentionSpan")))).first();

        double totalAttention = sums.isNullAt(0) ? 0.0 : sums.getDouble(0);
        double weightedSum = sums.isNullAt(1) ? 0.0 : sums.getDouble(1);

        if (totalAttention == 0)
            return 0.0;

        // Gini formula
        return (2.0 * weightedSum) / (n * totalAttention) - (n + 1.0) / n;
    }
}
