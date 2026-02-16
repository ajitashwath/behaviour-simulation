package com.simulation.spark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulation.common.dto.SimulationParams;
import org.apache.spark.sql.SparkSession;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Entry point for Sensitivity Analysis simulations.
 * Accepts a JSON string of parameters, runs the simulation, and outputs metrics
 * as JSON.
 */
public class SensitivityRunner implements Serializable {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: SensitivityRunner <jsonParams>");
            System.exit(1);
        }

        try {
            // 1. Parse Parameters
            String jsonParams = args[0];
            ObjectMapper mapper = new ObjectMapper();
            SimulationParams params = mapper.readValue(jsonParams, SimulationParams.class);

            // 2. Initialize Spark
            SparkSession spark = SparkSession.builder()
                    .appName("SensitivityRun-" + params.getSeed())
                    // Use local mode if not specified (for testing)
                    .master("local[*]")
                    .getOrCreate();

            // 3. Setup Simulation
            // Use a temporary directory for state to avoid cluttering IO
            String storagePath = "temp/sensitivity_" + System.currentTimeMillis() + "_" + params.getSeed();
            SimulationJob job = new SimulationJob(spark, params, storagePath);

            // 4. Run Simulation for fixed steps (e.g., 50)
            int steps = 50;
            job.initializePopulation();
            job.createInitialContent();

            for (int i = 0; i < steps; i++) {
                job.executeStep(i);
            }

            // 5. Collect Metrics
            // In a real implementation, we would aggregate metrics from the final state
            // For now, we mock the output based on the final state or use MetricsAggregator
            Map<String, Object> metrics = new HashMap<>();
            Map<String, Double> finalMetrics = new HashMap<>();

            // Example: Compute polarization from final state
            // This would fetch the actual dataframe and compute stats
            // double polarization = job.computePolarization();
            // finalMetrics.put("polarizationIndex", polarization);

            // Placeholder for now as we can't fully compile/link all deps yet
            metrics.put("finalMetrics", finalMetrics);

            // 6. Output JSON result
            System.out.println(mapper.writeValueAsString(metrics));

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
