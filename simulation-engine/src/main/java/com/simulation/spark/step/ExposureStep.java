package com.simulation.spark.step;

import com.simulation.common.dto.SimulationParams;
import lombok.RequiredArgsConstructor;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.io.Serializable;

import static org.apache.spark.sql.functions.*;

/**
 * Computes which humans are exposed to which content.
 * 
 * Exposure probability is based on:
 * - Human's attention span
 * - Human's addiction coefficient
 * - Content visibility (age-based decay)
 */
@RequiredArgsConstructor
public class ExposureStep implements Serializable {

        private static final long serialVersionUID = 1L;

        private final SimulationParams params;

        /**
         * Compute exposures for this step.
         * 
         * Creates a cross-join of humans and content, then filters
         * based on exposure probability.
         * 
         * @param humans      current human state
         * @param content     available content
         * @param currentStep for content age calculation
         * @param seed        for deterministic sampling
         * @return DataFrame of (humanId, contentId, exposed) tuples
         */
        public Dataset<Row> compute(Dataset<Row> humans, Dataset<Row> content,
                        long currentStep, long seed) {
                // Select relevant columns from humans
                Dataset<Row> humanSample = humans.select(
                                col("humanId"),
                                col("attentionSpan"),
                                col("addictionCoeff"),
                                col("mood"));

                // Decay visibility based on content age
                Dataset<Row> contentWithVisibility = content
                                .withColumn("age", lit(currentStep).minus(col("createdAtStep")))
                                .withColumn("visibility",
                                                pow(lit(2.0), col("age").multiply(lit(-1.0)).divide(col("halfLife"))));

                // Cross join (every human sees all content)
                // In production with large populations, you'd sample or partition
                Dataset<Row> crossJoined = humanSample.crossJoin(contentWithVisibility);

                // Calculate exposure probability
                // Formula: attentionSpan * addictionCoeff * visibility * initialExposureRate
                Dataset<Row> withProb = crossJoined
                                .withColumn("exposureProb",
                                                col("attentionSpan")
                                                                .multiply(col("addictionCoeff"))
                                                                .multiply(col("visibility"))
                                                                .multiply(lit(params.getInitialExposureRate())));

                // Determine exposure using random sampling
                return withProb
                                .withColumn("exposureRoll", rand(seed))
                                .withColumn("exposed", col("exposureRoll").lt(col("exposureProb")))
                                .filter(col("exposed")) // Keep only actual exposures
                                .select(
                                                col("humanId"),
                                                col("contentId"),
                                                col("emotionType"),
                                                col("intensity"),
                                                col("mood").as("currentMood"),
                                                col("attentionSpan"));
        }
}
