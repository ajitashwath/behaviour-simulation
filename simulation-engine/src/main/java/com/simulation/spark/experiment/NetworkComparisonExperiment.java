package com.simulation.spark.experiment;

import com.simulation.common.dto.NetworkType;
import com.simulation.common.dto.SimulationParams;
import com.simulation.spark.SimulationJob;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.SparkSession;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Full experimental framework for network topology comparison.
 * 
 * Research Question: How does network structure affect emotional contagion?
 * 
 * Experimental Design:
 * - 6 conditions (topologies) × 10 replicates = 60 simulation runs
 * - Population: 10,000 humans
 * - Duration: 100 steps
 * - Output: Labeled by topology and replicate for statistical analysis
 * 
 * Expected Runtime: ~2-4 hours on an 8-core machine
 * 
 * Usage:
 * java -jar simulation-engine.jar
 * com.simulation.spark.experiment.NetworkComparisonExperiment <output-dir>
 */
@Slf4j
public class NetworkComparisonExperiment {

    private static final int POPULATION_SIZE = 10_000;
    private static final int NUM_STEPS = 100;
    private static final int NUM_REPLICATES = 10;

    public static void main(String[] args) {
        String baseOutputDir = args.length > 0 ? args[0] : "./experiment-results";

        // Create timestamped experiment directory
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String experimentDir = Paths.get(baseOutputDir, "network_comparison_" + timestamp).toString();

        SparkSession spark = SparkSession.builder()
                .appName("NetworkComparisonExperiment")
                .master("local[*]")
                .config("spark.driver.memory", "8g")
                .config("spark.executor.memory", "8g")
                .getOrCreate();

        try {
            runExperiment(spark, experimentDir);
        } finally {
            spark.stop();
        }
    }

    private static void runExperiment(SparkSession spark, String experimentDir) {
        log.info("=".repeat(100));
        log.info("NETWORK TOPOLOGY COMPARISON EXPERIMENT");
        log.info("Research Question: How does network structure mediate emotional contagion?");
        log.info("=".repeat(100));
        log.info("Configuration:");
        log.info("  Population size: {}", POPULATION_SIZE);
        log.info("  Steps per run: {}", NUM_STEPS);
        log.info("  Replicates per condition: {}", NUM_REPLICATES);
        log.info("  Output directory: {}", experimentDir);
        log.info("=".repeat(100));

        // Define experimental conditions
        List<ExperimentalCondition> conditions = createConditions();

        log.info("\nExperimental Conditions ({} total):", conditions.size());
        for (int i = 0; i < conditions.size(); i++) {
            log.info("  {}. {}", i + 1, conditions.get(i).getName());
        }

        int totalRuns = conditions.size() * NUM_REPLICATES;
        int completedRuns = 0;
        long startTime = System.currentTimeMillis();

        // Run all combinations
        for (ExperimentalCondition condition : conditions) {
            log.info("\n" + "=".repeat(100));
            log.info("CONDITION: {}", condition.getName());
            log.info("=".repeat(100));

            for (int replicate = 0; replicate < NUM_REPLICATES; replicate++) {
                completedRuns++;

                log.info("\n  Replicate {}/{}", replicate + 1, NUM_REPLICATES);
                log.info("  Overall progress: {}/{} ({:.1f}%)",
                        completedRuns, totalRuns, (100.0 * completedRuns / totalRuns));

                try {
                    runSingleExperiment(spark, experimentDir, condition, replicate);

                    // Estimate time remaining
                    long elapsed = System.currentTimeMillis() - startTime;
                    long avgTimePerRun = elapsed / completedRuns;
                    long remaining = avgTimePerRun * (totalRuns - completedRuns);
                    log.info("  ✓ Completed. Estimated time remaining: {} minutes", remaining / 60000);

                } catch (Exception e) {
                    log.error("  ✗ FAILED: {}, replicate {}", condition.getName(), replicate, e);
                }
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("\n" + "=".repeat(100));
        log.info("EXPERIMENT COMPLETE");
        log.info("=".repeat(100));
        log.info("Total runs: {}", completedRuns);
        log.info("Total time: {} minutes", totalTime / 60000);
        log.info("Results saved to: {}", experimentDir);
        log.info("\nNext steps:");
        log.info("  1. Run statistical analysis (ANOVA, post-hoc tests)");
        log.info("  2. Generate publication figures");
        log.info("  3. Write results section of paper");
    }

    private static void runSingleExperiment(
            SparkSession spark,
            String experimentDir,
            ExperimentalCondition condition,
            int replicate) throws Exception {

        // Create unique storage path for this run
        String runDir = Paths.get(
                experimentDir,
                "data",
                condition.getName(),
                String.format("replicate_%02d", replicate)).toString();

        Files.createDirectories(Paths.get(runDir));

        // Create parameters with unique seed per replicate
        SimulationParams params = SimulationParams.builder()
                .seed(42L + replicate) // Different seed for each replicate
                .populationSize(POPULATION_SIZE)
                .contentCount(100)
                .networkType(condition.getNetworkType())
                .networkM(condition.getNetworkM())
                .networkK(condition.getNetworkK())
                .networkP(condition.getNetworkP())
                .rageSpreadMultiplier(1.1) // Key experimental manipulation
                .joySpreadMultiplier(1.0) // Baseline
                .build();

        // Run simulation
        SimulationJob job = new SimulationJob(spark, params, runDir);
        job.initializePopulation();

        for (int step = 0; step < NUM_STEPS; step++) {
            job.executeStep(step);
        }
    }

    private static List<ExperimentalCondition> createConditions() {
        List<ExperimentalCondition> conditions = new ArrayList<>();

        // Condition 1: Baseline (all-to-all)
        conditions.add(ExperimentalCondition.builder()
                .name("all_to_all")
                .networkType(NetworkType.ALL_TO_ALL)
                .build());

        // Condition 2: Barabási-Albert sparse (m=2)
        conditions.add(ExperimentalCondition.builder()
                .name("ba_m2")
                .networkType(NetworkType.BARABASI_ALBERT)
                .networkM(2)
                .build());

        // Condition 3: Barabási-Albert dense (m=5)
        conditions.add(ExperimentalCondition.builder()
                .name("ba_m5")
                .networkType(NetworkType.BARABASI_ALBERT)
                .networkM(5)
                .build());

        // Condition 4: Watts-Strogatz strong small-world (p=0.05)
        conditions.add(ExperimentalCondition.builder()
                .name("ws_p005")
                .networkType(NetworkType.WATTS_STROGATZ)
                .networkK(6)
                .networkP(0.05)
                .build());

        // Condition 5: Watts-Strogatz weak small-world (p=0.50)
        conditions.add(ExperimentalCondition.builder()
                .name("ws_p050")
                .networkType(NetworkType.WATTS_STROGATZ)
                .networkK(6)
                .networkP(0.50)
                .build());

        // Condition 6: Random graph (baseline control)
        conditions.add(ExperimentalCondition.builder()
                .name("random_k6")
                .networkType(NetworkType.RANDOM)
                .networkK(6)
                .build());

        return conditions;
    }

    @Data
    @Builder
    private static class ExperimentalCondition {
        private String name;
        private NetworkType networkType;

        @Builder.Default
        private int networkM = 3;

        @Builder.Default
        private int networkK = 6;

        @Builder.Default
        private double networkP = 0.05;
    }
}
