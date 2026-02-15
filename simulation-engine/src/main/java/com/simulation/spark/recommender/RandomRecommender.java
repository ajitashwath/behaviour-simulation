package com.simulation.spark.recommender;

import lombok.RequiredArgsConstructor;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import static org.apache.spark.sql.functions.*;

/**
 * Random Recommender (Baseline Control)
 * 
 * Randomly shuffles content with no optimization.
 * Serves as control condition to measure RL amplification effect.
 */
@RequiredArgsConstructor
public class RandomRecommender implements Recommender {

    private static final long serialVersionUID = 1L;

    @Override
    public Dataset<Row> rankContent(
            Dataset<Row> humans,
            Dataset<Row> content,
            Dataset<Row> interactions,
            long step) {

        long seed = step + 99999L;

        // Assign random scores
        Dataset<Row> randomScores = content
                .withColumn("score", rand(seed))
                .select(
                        col("contentId"),
                        col("emotionType"),
                        col("score"));

        // Cross-join with humans
        return humans
                .select(col("humanId"))
                .crossJoin(randomScores)
                .select(
                        col("humanId"),
                        col("contentId"),
                        col("emotionType"),
                        col("score"));
    }

    @Override
    public String getName() {
        return "Random";
    }
}
