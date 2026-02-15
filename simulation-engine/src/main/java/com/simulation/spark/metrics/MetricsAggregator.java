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

        long joyCount = moodDist.getOrDefault("JOY", 0L);
        long neutralCount = moodDist.getOrDefault("NEUTRAL", 0L);
        long rageCount = moodDist.getOrDefault("RAGE", 0L);

        // Interaction stats
        long totalInteractions = 0;
        long positiveInteractions = 0;
        long negativeInteractions = 0;

        if (!reactions.isEmpty()) {
            totalInteractions = reactions.count();
            positiveInteractions = reactions.filter(col("emotionType").equalTo("POSITIVE")).count();
            negativeInteractions = reactions.filter(col("emotionType").equalTo("NEGATIVE")).count();
        }

        // Engagement metrics
        double avgEngagement = population > 0 ? (double) totalInteractions / population : 0.0;
        double engagementGini = computeEngagementGini(humans, reactions);

        // Polarization metrics
        double polarizationIndex = population > 0 ? Math.abs(rageCount - joyCount) / (double) population : 0.0;

        double extremismRate = population > 0 ? (rageCount + joyCount) / (double) population : 0.0;

        double fragmentationIndex = population > 0 ? 1.0 - (neutralCount / (double) population) : 0.0;

        return Metrics.builder()
                .timeStep(timeStep)
                .moodDistribution(moodDist)
                .totalInteractions(totalInteractions)
                .positiveInteractions(positiveInteractions)
                .negativeInteractions(negativeInteractions)
                .avgEngagement(avgEngagement)
                .engagementGini(engagementGini)
                .polarizationIndex(polarizationIndex)
                .extremismRate(extremismRate)
                .fragmentationIndex(fragmentationIndex)
                .build();
    }

    private double computeEngagementGini(Dataset<Row> humans, Dataset<Row> reactions) {
        if (reactions.isEmpty()) {
            return 0.0;
        }

        // Count interactions per human
        Dataset<Row> engagementPerHuman = reactions
                .groupBy("humanId")
                .agg(count("*").as("engagement"));

        // Left join to include humans with zero engagement
        Dataset<Row> allEngagement = humans
                .select("humanId")
                .join(engagementPerHuman, "humanId", "left")
                .select(coalesce(col("engagement"), lit(0L)).as("engagement"));

        return computeGiniCoefficient(allEngagement, "engagement");
    }

    private double computeGiniCoefficient(Dataset<Row> data, String column) {
        Dataset<Row> sorted = data
                .select(column)
                .orderBy(column)
                .withColumn("idx", monotonically_increasing_id());

        long n = sorted.count();
        if (n == 0)
            return 0.0;

        Row sums = sorted.agg(
                sum(column),
                sum(col("idx").multiply(col(column)))).first();

        double total = sums.isNullAt(0) ? 0.0 : sums.getDouble(0);
        double weightedSum = sums.isNullAt(1) ? 0.0 : sums.getDouble(1);

        if (total == 0)
            return 0.0;

        return (2.0 * weightedSum) / (n * total) - (n + 1.0) / n;
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

}
