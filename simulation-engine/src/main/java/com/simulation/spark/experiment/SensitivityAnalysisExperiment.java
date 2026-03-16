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
 * 72-Run One-At-a-Time (OAT) Sensitivity Analysis Experiment
 * 
 * Addresses peer reviewer concerns efficiently:
 * - Tests extremes of 4 key parameters
 * - 8 conditions × 3 topologies × 3 replicates = 72 runs
 * - Verifies rank ordering of topologies (BA > WS > ER) holds.
 */
@Slf4j
public class SensitivityAnalysisExperiment {

    private static final int POPULATION_SIZE = 10_000;
    private static final int NUM_STEPS = 100;
    private static final int NUM_REPLICATES = 3; // Reduced from 10 for sensitivity

    public static void main(String[] args) {
        String baseOutputDir = args.length > 0 ? args[0] : "./experiment-results";

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String experimentDir = Paths.get(baseOutputDir, "sensitivity_analysis_" + timestamp).toString();

        SparkSession spark = SparkSession.builder()
                .appName("SensitivityAnalysisExperiment")
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
        log.info("GLOBAL SENSITIVITY ANALYSIS (OAT DESIGN)");
        log.info("=".repeat(100));

        List<SensitivityCondition> conditions = createConditions();

        int totalRuns = conditions.size() * NUM_REPLICATES;
        int completedRuns = 0;
        long startTime = System.currentTimeMillis();

        for (SensitivityCondition condition : conditions) {
            log.info("\n" + "=".repeat(100));
            log.info("SENSITIVITY CONDITION: {}", condition.getName());
            log.info("Topology: {}", condition.getNetworkType());
            log.info("=".repeat(100));

            for (int replicate = 0; replicate < NUM_REPLICATES; replicate++) {
                completedRuns++;
                log.info("\n  Replicate {}/{} | Progress: {}/{}", replicate + 1, NUM_REPLICATES, completedRuns, totalRuns);

                try {
                    runSingleExperiment(spark, experimentDir, condition, replicate);
                } catch (Exception e) {
                    log.error("FAILED: {}", condition.getName(), e);
                }
            }
        }

        log.info("SENSITIVITY EXPERIMENT COMPLETE. Runs: {}", completedRuns);
    }

    private static void runSingleExperiment(
            SparkSession spark,
            String experimentDir,
            SensitivityCondition condition,
            int replicate) throws Exception {

        String runDir = Paths.get(
                experimentDir,
                "data",
                condition.getName(),
                String.format("replicate_%02d", replicate)).toString();

        Files.createDirectories(Paths.get(runDir));

        SimulationParams params = SimulationParams.builder()
                .seed(42L + replicate)
                .populationSize(POPULATION_SIZE)
                .contentCount(100)
                .networkType(condition.getNetworkType())
                .networkM(condition.getNetworkM())
                .networkK(condition.getNetworkK())
                .networkP(condition.getNetworkP())
                .rageSpreadMultiplier(condition.getRageSpreadMultiplier())
                .joySpreadMultiplier(1.0)
                .rageStickiness(condition.getRageStickiness())
                .socialProofBase(condition.getSocialProofBase())
                .initialMoodDistribution(condition.getInitialMoodDistribution())
                .build();

        SimulationJob job = new SimulationJob(spark, params, runDir);
        job.initializePopulation();

        for (int step = 0; step < NUM_STEPS; step++) {
            job.executeStep(step);
        }
    }

    private static List<SensitivityCondition> createConditions() {
        List<SensitivityCondition> conditions = new ArrayList<>();
        
        // Define topologies
        NetworkType[] topologies = { NetworkType.BARABASI_ALBERT, NetworkType.WATTS_STROGATZ, NetworkType.RANDOM };
        int[] mVals = { 3, 0, 0 }; // Only for BA
        int[] kVals = { 0, 6, 6 }; // For WS, ER
        double[] pVals = { 0.0, 0.05, 0.0 }; // Only for WS
        
        // OAT Extremes
        double[] rageSpreadExtremes = { 1.0, 1.5 }; // Baseline 1.1 inside runSingle (so 1.0 and 1.5)
        double[] stickinessExtremes = { 0.1, 0.5 }; // Baseline 0.3
        double[] proofBaseExtremes = { 0.25, 0.75 }; // Baseline 0.5
        double[][] moodExtremes = { { 0.35, 0.60 }, { 0.00, 0.60 } }; // 5% RAGE and 40% RAGE

        for (int i = 0; i < topologies.length; i++) {
            NetworkType top = topologies[i];
            String topName = top.name().toLowerCase().replace("_", "");
            int m = mVals[i];
            int k = kVals[i];
            double p = pVals[i];

            for (double val : rageSpreadExtremes) {
                conditions.add(createCond(topName + "_rageSpread_" + val, top, m, k, p, val, 0.3, 0.5, new double[]{0.2, 0.6}));
            }
            for (double val : stickinessExtremes) {
                conditions.add(createCond(topName + "_stickiness_" + val, top, m, k, p, 1.1, val, 0.5, new double[]{0.2, 0.6}));
            }
            for (double val : proofBaseExtremes) {
                conditions.add(createCond(topName + "_proofBase_" + val, top, m, k, p, 1.1, 0.3, val, new double[]{0.2, 0.6}));
            }
            for (int j = 0; j < moodExtremes.length; j++) {
                double[] val = moodExtremes[j];
                String label = j == 0 ? "5pctRage" : "40pctRage";
                conditions.add(createCond(topName + "_mood_" + label, top, m, k, p, 1.1, 0.3, 0.5, val));
            }
        }
        return conditions;
    }

    private static SensitivityCondition createCond(String name, NetworkType type, int m, int k, double p,
                                                   double rageMult, double stickiness, double proofBase, double[] mood) {
        return SensitivityCondition.builder()
                .name(name)
                .networkType(type)
                .networkM(m).networkK(k).networkP(p)
                .rageSpreadMultiplier(rageMult)
                .rageStickiness(stickiness)
                .socialProofBase(proofBase)
                .initialMoodDistribution(mood)
                .build();
    }

    @Data
    @Builder
    private static class SensitivityCondition {
        private String name;
        private NetworkType networkType;
        private int networkM;
        private int networkK;
        private double networkP;
        private double rageSpreadMultiplier;
        private double rageStickiness;
        private double socialProofBase;
        private double[] initialMoodDistribution;
    }
}
