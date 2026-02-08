package com.simulation.spark.step;

import com.simulation.common.dto.SimulationParams;
import lombok.RequiredArgsConstructor;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.io.Serializable;

import static org.apache.spark.sql.functions.*;

/**
 * Updates human mood based on emotional contagion.
 * 
 * Contagion model:
 * - Each mood has a "spread rate" based on the multiplier
 * - Humans in NEUTRAL state are susceptible
 * - JOY and RAGE can convert each other at reduced rates
 * - Rage is "sticky" - harder to lose once acquired
 */
@RequiredArgsConstructor
public class MoodContagionStep implements Serializable {

    private static final long serialVersionUID = 1L;

    private final SimulationParams params;

    /**
     * Apply mood contagion to the population.
     * 
     * @param humans    current human state
     * @param reactions reactions from this step (affect individual mood)
     * @param seed      for deterministic random
     * @return humans with updated moods
     */
    public Dataset<Row> apply(Dataset<Row> humans, Dataset<Row> reactions, long seed) {
        // Count current mood distribution for population-level contagion
        long totalCount = humans.count();

        // Get mood counts
        Dataset<Row> moodCounts = humans.groupBy("mood").count();

        // Extract counts (with defaults for safety)
        long joyCount = getMoodCount(moodCounts, "JOY");
        long rageCount = getMoodCount(moodCounts, "RAGE");

        // Calculate contagion probabilities
        double joyContagionProb = (double) joyCount / totalCount
                * params.getJoySpreadMultiplier()
                * 0.1; // Base contagion rate
        double rageContagionProb = (double) rageCount / totalCount
                * params.getRageSpreadMultiplier()
                * 0.1;

        // Aggregate emotion deltas per human from reactions
        Dataset<Row> emotionAgg = reactions
                .groupBy("humanId")
                .agg(sum("emotionDelta").as("totalEmotionDelta"));

        // Join with humans
        Dataset<Row> humansWithDelta = humans
                .join(emotionAgg, humans.col("humanId").equalTo(emotionAgg.col("humanId")), "left")
                .drop(emotionAgg.col("humanId"))
                .na().fill(0.0, new String[] { "totalEmotionDelta" });

        // Apply mood transitions
        return humansWithDelta
                .withColumn("contagionRoll", rand(seed + 200))
                // Mood transition logic
                .withColumn("newMood",
                        // NEUTRAL can transition to either JOY or RAGE
                        when(col("mood").equalTo("NEUTRAL"),
                                when(col("contagionRoll").lt(rageContagionProb), lit("RAGE"))
                                        .when(col("contagionRoll").lt(rageContagionProb + joyContagionProb), lit("JOY"))
                                        // Also consider individual emotion delta
                                        .when(col("totalEmotionDelta").gt(0.2), lit("JOY"))
                                        .when(col("totalEmotionDelta").lt(-0.2), lit("RAGE"))
                                        .otherwise(lit("NEUTRAL")))
                                // JOY can become NEUTRAL (then potentially RAGE)
                                .when(col("mood").equalTo("JOY"),
                                        when(col("contagionRoll").lt(rageContagionProb * 0.5), lit("NEUTRAL"))
                                                .when(col("totalEmotionDelta").lt(-0.3), lit("NEUTRAL"))
                                                .otherwise(lit("JOY")))
                                // RAGE is sticky - hard to lose
                                .when(col("mood").equalTo("RAGE"),
                                        when(col("contagionRoll").lt(joyContagionProb * 0.3), lit("NEUTRAL"))
                                                .when(col("totalEmotionDelta").gt(0.4), lit("NEUTRAL"))
                                                .otherwise(lit("RAGE")))
                                .otherwise(col("mood")))
                .drop("mood", "contagionRoll", "totalEmotionDelta")
                .withColumnRenamed("newMood", "mood");
    }

    private long getMoodCount(Dataset<Row> moodCounts, String mood) {
        Row[] rows = (Row[]) moodCounts.filter(col("mood").equalTo(mood)).collect();
        return rows.length > 0 ? rows[0].getLong(1) : 0L;
    }
}
