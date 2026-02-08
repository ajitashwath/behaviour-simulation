package com.simulation.spark.step;

import com.simulation.common.dto.SimulationParams;
import lombok.RequiredArgsConstructor;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.io.Serializable;

import static org.apache.spark.sql.functions.*;

/**
 * Applies attention decay and recovery.
 * 
 * Attention model:
 * - Decays by a fixed rate each step
 * - Recovers when below threshold (simulating rest)
 * - Bounded to [0, 1]
 */
@RequiredArgsConstructor
public class AttentionDecayStep implements Serializable {

    private static final long serialVersionUID = 1L;

    private final SimulationParams params;

    /**
     * Apply attention decay and recovery.
     * 
     * @param humans DataFrame with current attention values
     * @return DataFrame with updated attention
     */
    public Dataset<Row> apply(Dataset<Row> humans) {
        double decayRate = params.getAttentionDecayRate();
        double recoveryRate = params.getAttentionRecoveryRate();

        return humans
                .withColumn("newAttention",
                        // Apply decay
                        col("attentionSpan").multiply(lit(1.0 - decayRate))
                                // Add recovery if below threshold
                                .plus(
                                        when(col("attentionSpan").lt(0.5), lit(recoveryRate))
                                                .otherwise(lit(0.0))))
                // Clamp to [0, 1]
                .withColumn("attentionSpan",
                        when(col("newAttention").gt(1.0), lit(1.0))
                                .when(col("newAttention").lt(0.0), lit(0.0))
                                .otherwise(col("newAttention")))
                .drop("newAttention");
    }
}
