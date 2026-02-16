package com.simulation.spark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulation.common.dto.SimulationParams;
import org.apache.spark.sql.SparkSession;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Entry point for Calibration simulations.
 * Similar to SensitivityRunner but optimized for ABC (Approximate Bayesian
 * Computation).
 * Outputs specific summary statistics required for distance calculation.
 */
public class CalibrationRunner implements Serializable {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: CalibrationRunner <jsonParams>");
            System.exit(1);
        }

        try {
            // 1. Parse Parameters
            String jsonParams = args[0];
            ObjectMapper mapper = new ObjectMapper();
            SimulationParams params = mapper.readValue(jsonParams, SimulationParams.class);

            // 2. Initialize Spark
            SparkSession spark = SparkSession.builder()
                    .appName("CalibrationRun-" + params.getSeed())
                    .master("local[*]")
                    .getOrCreate();

            // 3. Setup Simulation
            String storagePath = "temp/calibration_" + System.currentTimeMillis() + "_" + params.getSeed();
            SimulationJob job = new SimulationJob(spark, params, storagePath);

            // 4. Run Simulation
            // Calibration might require longer runs to reach steady state
            int steps = 100;
            job.initializePopulation();
            job.createInitialContent();

            for (int i = 0; i < steps; i++) {
                job.executeStep(i);
            }

            // 5. Collect Summary Statistics
            Map<String, Object> metrics = new HashMap<>();

            // In a real implementation, we'd compute these from the DataFrames
            // Mocking for now to match the ABCEngine expectations
            // These would be computed by MetricsAggregator usually

            Map<String, Object> summaryStats = new HashMap<>();

            // Mock mood distribution [Neg, Neu, Pos]
            // In reality: humansDF.groupBy("mood").count() -> normalize
            double[] moodDist = { 0.2, 0.5, 0.3 };
            summaryStats.put("mood_distribution", moodDist);

            // Mock polarization
            summaryStats.put("polarization_index", 0.45);

            // Mock transition matrix (flat list or nested list)
            double[][] transMatrix = {
                    { 0.7, 0.2, 0.1 },
                    { 0.1, 0.8, 0.1 },
                    { 0.1, 0.3, 0.6 }
            };
            summaryStats.put("transition_matrix", transMatrix);

            metrics.put("summaryStatistics", summaryStats);

            // 6. Output JSON result
            System.out.println(mapper.writeValueAsString(metrics));

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
