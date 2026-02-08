package com.simulation.spark.step;

import com.simulation.common.dto.SimulationParams;
import lombok.RequiredArgsConstructor;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.io.Serializable;

import static org.apache.spark.sql.functions.*;

/**
 * Accumulates fatigue based on engagement.
 * 
 * Fatigue model:
 * - Increases with each reaction
 * - Slowly recovers when not reacting
 * - When near 1.0, human becomes "collapsed" (minimal engagement)
 */
@RequiredArgsConstructor
public class FatigueStep implements Serializable {

    private static final long serialVersionUID = 1L;

    private final SimulationParams params;

    /**
     * Apply fatigue changes.
     * 
     * @param humans    DataFrame with current fatigue values
     * @param reactions reactions from this step
     * @return DataFrame with updated fatigue
     */
    public Dataset<Row> apply(Dataset<Row> humans, Dataset<Row> reactions) {
        double accumulationRate = params.getFatigueAccumulationRate();
        double recoveryRate = params.getFatigueRecoveryRate();

        // Count reactions per human
        Dataset<Row> reactionCounts = reactions
                .filter(col("reacted"))
                .groupBy("humanId")
                .count()
                .withColumnRenamed("count", "reactionCount");

        // Join with humans
        Dataset<Row> joined = humans
                .join(reactionCounts,
                        humans.col("humanId").equalTo(reactionCounts.col("humanId")),
                        "left")
                .drop(reactionCounts.col("humanId"))
                .na().fill(0L, new String[] { "reactionCount" });

        return joined
                .withColumn("newFatigue",
                        // Increase from reactions
                        col("fatigue").plus(
                                col("reactionCount").cast("double").multiply(lit(accumulationRate)))
                                // Decrease from recovery (if no reactions)
                                .minus(
                                        when(col("reactionCount").equalTo(0), lit(recoveryRate))
                                                .otherwise(lit(0.0))))
                // Clamp to [0, 1]
                .withColumn("fatigue",
                        when(col("newFatigue").gt(1.0), lit(1.0))
                                .when(col("newFatigue").lt(0.0), lit(0.0))
                                .otherwise(col("newFatigue")))
                .drop("newFatigue", "reactionCount");
    }
}
