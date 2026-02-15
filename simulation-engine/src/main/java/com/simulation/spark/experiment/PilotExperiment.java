package com.simulation.spark.experiment;

import com.simulation.common.dto.NetworkType;
import com.simulation.common.dto.SimulationParams;
import com.simulation.spark.SimulationJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.SparkSession;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Pilot experiment to test network generation and basic simulation
 * functionality.
 * 
 * Tests all network topologies with a small population (1000 nodes, 20 steps)
 * to verify:
 * - Network generation succeeds
 * - Network metrics are computed correctly
 * - Simulation runs without errors
 * - Network-based exposure produces results
 * 
 * Run this before full experimental runs to catch issues early.
 */
@Slf4j
public class PilotExperiment {

    public static void main(String[] args) {
        String baseStoragePath = args.length > 0 ? args[0] : "./pilot-results";

        SparkSession spark = SparkSession.builder()
                .appName("PilotExperiment")
                .master("local[*]") // Use all local cores
                .config("spark.driver.memory", "4g")
                .getOrCreate();

        try {
            runPilot(spark, baseStoragePath);
        } finally {
            spark.stop();
        }
    }

    private static void runPilot(SparkSession spark, String baseStoragePath) {
        log.info("=".repeat(80));
        log.info("PILOT EXPERIMENT: Network Topology Verification");
        log.info("=".repeat(80));

        // Test configurations
        NetworkType[] topologies = {
                NetworkType.ALL_TO_ALL,
                NetworkType.BARABASI_ALBERT,
                NetworkType.WATTS_STROGATZ,
                NetworkType.RANDOM
        };

        int populationSize = 1000;
        int numSteps = 20;

        for (NetworkType topology : topologies) {
            log.info("\n" + "-".repeat(80));
            log.info("Testing topology: {}", topology);
            log.info("-".repeat(80));

            try {
                String storagePath = Paths.get(baseStoragePath, topology.name().toLowerCase()).toString();

                // Create storage directory
                Files.createDirectories(Paths.get(storagePath));

                // Create simulation parameters
                SimulationParams params = SimulationParams.builder()
                        .seed(42L)
                        .populationSize(populationSize)
                        .contentCount(50)
                        .networkType(topology)
                        .networkM(3) // For Barabási-Albert
                        .networkK(6) // For Watts-Strogatz and Random
                        .networkP(0.05) // For Watts-Strogatz
                        .rageSpreadMultiplier(1.1)
                        .joySpreadMultiplier(1.0)
                        .build();

                // Create job and initialize
                SimulationJob job = new SimulationJob(spark, params, storagePath);

                log.info("Initializing population...");
                job.initializePopulation();

                log.info("Running {} steps...", numSteps);
                for (int step = 0; step < numSteps; step++) {
                    job.executeStep(step);
                    if ((step + 1) % 5 == 0) {
                        log.info("  Completed step {}/{}", step + 1, numSteps);
                    }
                }

                log.info("✓ SUCCESS: {} topology test completed", topology);
                log.info("  Results saved to: {}", storagePath);

            } catch (Exception e) {
                log.error("✗ FAILED: {} topology test", topology, e);
                log.error("  Error: {}", e.getMessage());
            }
        }

        log.info("\n" + "=".repeat(80));
        log.info("PILOT EXPERIMENT COMPLETE");
        log.info("=".repeat(80));
        log.info("Review results in: {}", baseStoragePath);
        log.info("Next step: Run statistical analysis on pilot data");
    }
}
