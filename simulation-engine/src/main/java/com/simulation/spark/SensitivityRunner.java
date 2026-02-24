package com.simulation.spark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulation.common.dto.SimulationParams;
import org.apache.spark.sql.SparkSession;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SensitivityRunner implements Serializable {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: SensitivityRunner <jsonParams>");
            System.exit(1);
        }

        try {
            String jsonParams = args[0];
            ObjectMapper mapper = new ObjectMapper();
            SimulationParams params = mapper.readValue(jsonParams, SimulationParams.class);

            SparkSession spark = SparkSession.builder()
                    .appName("SensitivityRun-" + params.getSeed())
                    .master("local[*]")
                    .getOrCreate();

            String storagePath = "temp/sensitivity_" + System.currentTimeMillis() + "_" + params.getSeed();
            SimulationJob job = new SimulationJob(spark, params, storagePath);

            int steps = 50;
            job.initializePopulation();
            job.createInitialContent();

            for (int i = 0; i < steps; i++) {
                job.executeStep(i);
            }

            Map<String, Object> metrics = new HashMap<>();
            Map<String, Double> finalMetrics = new HashMap<>();

            metrics.put("finalMetrics", finalMetrics);

            System.out.println(mapper.writeValueAsString(metrics));

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
