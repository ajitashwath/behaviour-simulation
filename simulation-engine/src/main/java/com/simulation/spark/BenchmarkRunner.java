package com.simulation.spark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulation.common.dto.SimulationParams;
import org.apache.spark.sql.SparkSession;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Entry point for Performance Benchmarks.
 * Optimized for minimal overhead to measure core engine performance.
 */
public class BenchmarkRunner implements Serializable {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: BenchmarkRunner <jsonParams>");
            System.exit(1);
        }

        try {
            // 1. Parse Parameters
            String jsonParams = args[0];
            ObjectMapper mapper = new ObjectMapper();
            SimulationParams params = mapper.readValue(jsonParams, SimulationParams.class);

            // 2. Initialize Spark with performance tuning
            SparkSession spark = SparkSession.builder()
                    .appName("Benchmark-" + params.getPopulationSize())
                    // Master is usually set by spark-submit in real benchmarks
                    // .master("local[*]")
                    .getOrCreate();

            long startTime = System.nanoTime();

            // 3. Setup Simulation
            String storagePath = "temp/benchmark_" + System.currentTimeMillis();
            SimulationJob job = new SimulationJob(spark, params, storagePath);

            // 4. Run Steps
            int steps = 50; // Standard benchmark length

            job.initializePopulation();
            job.createInitialContent();
            long initTime = System.nanoTime();

            for (int i = 0; i < steps; i++) {
                job.executeStep(i);
            }

            long endTime = System.nanoTime();

            // 5. Collect Metrics
            double totalTimeSec = (endTime - startTime) / 1e9;
            double avgStepTime = ((endTime - initTime) / steps) / 1e6; // ms
            double throughput = (params.getPopulationSize() * steps) / totalTimeSec;

            Map<String, Object> results = new HashMap<>();
            results.put("population", params.getPopulationSize());
            results.put("steps", steps);
            results.put("totalTimeSec", totalTimeSec);
            results.put("avgStepTimeMs", avgStepTime);
            results.put("throughput", throughput);

            // 6. Output JSON result
            System.out.println(mapper.writeValueAsString(results));

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
