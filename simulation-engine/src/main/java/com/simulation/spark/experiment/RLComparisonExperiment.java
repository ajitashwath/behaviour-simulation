package com.simulation.spark.experiment;

import com.simulation.common.dto.NetworkType;
import com.simulation.common.dto.RecommenderType;
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
 * Experimental framework for evaluating RL recommender effects on polarization.
 * 
 * Research Question: Do RL recommenders amplify polarization compared to random
 * baselines?
 * 
 * Experimental Design:
 * - 2 conditions (Thompson Sampling vs Random) × 10 replicates = 20 simulation
 * runs
 * - Population: 1,000 humans (pilot scale)
 * - Duration: 50 steps
 * - Output: Labeled by algorithmic condition and replicate
 * 
 * Usage:
 * java -cp simulation-engine.jar
 * com.simulation.spark.experiment.RLComparisonExperiment <output-dir>
 */
@Slf4j
public class RLComparisonExperiment {

    private static final int POPULATION_SIZE = 1_000;
    private static final int NUM_STEPS = 50;
    private static final int NUM_REPLICATES = 10;

    public static void main(String[] args) {
        String baseOutputDir = args.length > 0 ? args[0] : "./rl-experiment-results";

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String experimentDir = Paths.get(baseOutputDir, "rl_comparison_" + timestamp).toString();

        SparkSession spark = SparkSession.builder()
                .appName("RLComparisonExperiment")
                .master("local[*]")
                .config("spark.driver.memory", "4g")
                .config("spark.executor.memory", "4g")
                .getOrCreate();

        try {
            runExperiment(spark, experimentDir);
        } finally {
            spark.stop();
        }
    }

    private static void runExperiment(SparkSession spark, String experimentDir) {
        log.info("=".repeat(100));
        log.info("RL RECOMMENDER COMPARISON EXPERIMENT");
        log.info("Research Question: Do RL recommenders amplify polarization?");
        log.info("=".repeat(100));
        log.info("Configuration:");
        log.info("  Population size: {}", POPULATION_SIZE);
        log.info("  Steps per run: {}", NUM_STEPS);
        log.info("  Replicates per condition: {}", NUM_REPLICATES);
        log.info("  Output directory: {}", experimentDir);
        log.info("=".repeat(100));

        List<ExperimentalCondition> conditions = createConditions();

        int totalRuns = conditions.size() * NUM_REPLICATES;
        int completedRuns = 0;
        long startTime = System.currentTimeMillis();

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
    }

    private static void runSingleExperiment(
            SparkSession spark,
            String experimentDir,
            ExperimentalCondition condition,
            int replicate) throws Exception {

        String runDir = Paths.get(
                experimentDir,
                "data",
                condition.getName(),
                String.format("replicate_%02d", replicate)).toString();

        Files.createDirectories(Paths.get(runDir));

        SimulationParams params = SimulationParams.builder()
                .seed(100L + replicate)
                .populationSize(POPULATION_SIZE)
                .contentCount(50)
                .networkType(NetworkType.BARABASI_ALBERT) // Must use a network topology to enable NetworkExposureStep
                                                          // which uses the recommender
                .recommenderType(condition.getRecommenderType())
                .rageSpreadMultiplier(1.1)
                .joySpreadMultiplier(1.0)
                .build();

        SimulationJob job = new SimulationJob(spark, params, runDir);
        job.initializePopulation();

        for (int step = 0; step < NUM_STEPS; step++) {
            job.executeStep(step);
        }
    }

    private static List<ExperimentalCondition> createConditions() {
        List<ExperimentalCondition> conditions = new ArrayList<>();

        conditions.add(ExperimentalCondition.builder()
                .name("baseline_random")
                .recommenderType(RecommenderType.RANDOM)
                .build());

        conditions.add(ExperimentalCondition.builder()
                .name("rl_thompson_sampling")
                .recommenderType(RecommenderType.THOMPSON_SAMPLING)
                .build());

        return conditions;
    }

    @Data
    @Builder
    private static class ExperimentalCondition {
        private String name;
        private RecommenderType recommenderType;
    }
}
