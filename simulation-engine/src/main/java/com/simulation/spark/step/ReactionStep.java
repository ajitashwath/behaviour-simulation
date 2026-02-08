package com.simulation.spark.step;

import com.simulation.common.dto.SimulationParams;
import lombok.RequiredArgsConstructor;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.io.Serializable;

import static org.apache.spark.sql.functions.*;

/**
 * Calculates reaction probabilities for exposed humans.
 * 
 * Reaction probability formula:
 * P(react) = baseReactionProb * (1 - fatigue) * attentionSpan *
 * contentIntensity
 * 
 * Reactions amplify content spread and have stronger mood effects.
 */
@RequiredArgsConstructor
public class ReactionStep implements Serializable {

    private static final long serialVersionUID = 1L;

    private final SimulationParams params;

    /**
     * Compute reactions from exposures.
     * 
     * @param exposures DataFrame from ExposureStep
     * @param seed      for deterministic sampling
     * @return DataFrame with reaction outcomes
     */
    public Dataset<Row> compute(Dataset<Row> exposures, long seed) {
        return exposures
                // Calculate effective reaction probability
                .withColumn("reactionProb",
                        col("attentionSpan")
                                .multiply(col("intensity"))
                                .multiply(lit(0.5))) // Base rate
                // Determine reaction
                .withColumn("reactionRoll", rand(seed + 100))
                .withColumn("reacted", col("reactionRoll").lt(col("reactionProb")))
                // Calculate emotion delta based on content type and reaction
                .withColumn("emotionDelta",
                        when(col("reacted"),
                                when(col("emotionType").equalTo("POSITIVE"), lit(0.1))
                                        .when(col("emotionType").equalTo("NEGATIVE"), lit(-0.15))
                                        .otherwise(lit(0.0)))
                                .otherwise(
                                        when(col("emotionType").equalTo("POSITIVE"), lit(0.02))
                                                .when(col("emotionType").equalTo("NEGATIVE"), lit(-0.03))
                                                .otherwise(lit(0.0))))
                .select(
                        col("humanId"),
                        col("contentId"),
                        col("emotionType"),
                        col("reacted"),
                        col("emotionDelta"),
                        col("currentMood"));
    }
}
