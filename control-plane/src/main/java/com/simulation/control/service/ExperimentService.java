package com.simulation.control.service;

import com.simulation.common.dto.CreateExperimentRequest;
import com.simulation.common.dto.ExperimentResponse;
import com.simulation.common.dto.SimulationParams;
import com.simulation.common.dto.StepRequest;
import com.simulation.common.model.Human;
import com.simulation.common.model.Metrics;
import com.simulation.common.model.Mood;
import com.simulation.control.config.SimulationConfig;
import com.simulation.control.entity.Experiment;
import com.simulation.control.entity.Experiment.ExperimentStatus;
import com.simulation.control.exception.ExperimentNotFoundException;
import com.simulation.control.exception.SimulationException;
import com.simulation.control.repository.ExperimentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Core service for experiment lifecycle management.
 * 
 * Handles:
 * - Experiment creation and initialization
 * - Step execution via Spark
 * - State and metrics retrieval
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExperimentService {

    private final ExperimentRepository experimentRepository;
    private final SimulationConfig simulationConfig;
    private final SparkSession sparkSession;

    /**
     * Create a new experiment.
     * 
     * Initializes experiment metadata and generates initial population state.
     * 
     * @param request creation request with name and parameters
     * @return the created experiment response
     */
    @Transactional
    public ExperimentResponse createExperiment(CreateExperimentRequest request) {
        log.info("Creating experiment: {}", request.getName());

        // Use provided params or defaults
        SimulationParams params = request.getParams() != null
                ? request.getParams()
                : SimulationParams.baseline();

        // Generate unique storage path
        String storagePath = UUID.randomUUID().toString();
        Path fullPath = Paths.get(simulationConfig.getStorage().getBasePath(), storagePath);

        // Create directory structure
        try {
            Files.createDirectories(fullPath.resolve("state"));
            Files.createDirectories(fullPath.resolve("metrics"));
            Files.createDirectories(fullPath.resolve("interactions"));
        } catch (IOException e) {
            throw new SimulationException("Failed to create storage directories", e);
        }

        // Create experiment entity
        Experiment experiment = Experiment.builder()
                .name(request.getName())
                .description(request.getDescription())
                .params(params)
                .storagePath(storagePath)
                .status(ExperimentStatus.CREATED)
                .currentStep(0L)
                .build();

        experiment = experimentRepository.save(experiment);

        // Initialize population state
        initializeState(experiment, params);

        log.info("Experiment created: id={}, name={}", experiment.getId(), experiment.getName());

        return toResponse(experiment);
    }

    /**
     * Execute simulation steps.
     * 
     * @param experimentId experiment to step
     * @param request      step configuration
     * @return updated experiment state
     */
    @Transactional
    public ExperimentResponse executeStep(UUID experimentId, StepRequest request) {
        Experiment experiment = getExperimentOrThrow(experimentId);

        log.info("Executing {} steps for experiment: {}", request.getSteps(), experimentId);

        // Validate state
        if (experiment.getStatus() == ExperimentStatus.COMPLETED) {
            throw new SimulationException("Experiment is already completed");
        }
        if (experiment.getStatus() == ExperimentStatus.FAILED) {
            throw new SimulationException("Experiment is in failed state");
        }

        experiment.setStatus(ExperimentStatus.RUNNING);
        experimentRepository.save(experiment);

        try {
            // Execute each step
            for (int i = 0; i < request.getSteps(); i++) {
                executeSingleStep(experiment);
                experiment.setCurrentStep(experiment.getCurrentStep() + 1);
                experimentRepository.save(experiment);
            }

            experiment.setStatus(ExperimentStatus.PAUSED);
            experimentRepository.save(experiment);

            log.info("Completed {} steps. Current step: {}",
                    request.getSteps(), experiment.getCurrentStep());

        } catch (Exception e) {
            experiment.setStatus(ExperimentStatus.FAILED);
            experimentRepository.save(experiment);
            throw new SimulationException("Step execution failed", e);
        }

        return toResponse(experiment);
    }

    /**
     * Get current population state.
     */
    public ExperimentResponse getState(UUID experimentId) {
        Experiment experiment = getExperimentOrThrow(experimentId);
        ExperimentResponse response = toResponse(experiment);

        // Load population summary from Parquet
        try {
            response.setPopulationSummary(loadPopulationSummary(experiment));
        } catch (Exception e) {
            log.warn("Could not load population summary", e);
        }

        return response;
    }

    /**
     * Get metrics history.
     */
    public ExperimentResponse getMetrics(UUID experimentId) {
        Experiment experiment = getExperimentOrThrow(experimentId);
        ExperimentResponse response = toResponse(experiment);

        // Load metrics from Parquet
        try {
            response.setMetricsHistory(loadMetricsHistory(experiment));
        } catch (Exception e) {
            log.warn("Could not load metrics history", e);
        }

        return response;
    }

    /**
     * List all experiments.
     */
    public List<ExperimentResponse> listExperiments() {
        return experimentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ========== Private Methods ==========

    private Experiment getExperimentOrThrow(UUID experimentId) {
        return experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ExperimentNotFoundException(experimentId.toString()));
    }

    /**
     * Initialize population state.
     * 
     * Creates initial humans with configured mood distribution.
     */
    private void initializeState(Experiment experiment, SimulationParams params) {
        log.info("Initializing state for {} humans", params.getPopulationSize());

        Random random = new Random(params.getSeed());
        List<Human> humans = new ArrayList<>();

        // Calculate mood distribution
        double joyFraction = params.getInitialMoodDistribution()[0];
        double neutralFraction = params.getInitialMoodDistribution()[1];
        // rage = 1 - joy - neutral

        for (int i = 0; i < params.getPopulationSize(); i++) {
            // Determine initial mood based on distribution
            double roll = random.nextDouble();
            Mood mood;
            if (roll < joyFraction) {
                mood = Mood.JOY;
            } else if (roll < joyFraction + neutralFraction) {
                mood = Mood.NEUTRAL;
            } else {
                mood = Mood.RAGE;
            }

            Human human = Human.builder()
                    .humanId(i)
                    .mood(mood)
                    // Random traits with some variation
                    .attentionSpan(0.5 + random.nextDouble() * 0.5) // [0.5, 1.0]
                    .addictionCoeff(0.3 + random.nextDouble() * 0.5) // [0.3, 0.8]
                    .reactionProb(0.3 + random.nextDouble() * 0.4) // [0.3, 0.7]
                    .fatigue(random.nextDouble() * 0.1) // [0.0, 0.1]
                    .build();

            humans.add(human);
        }

        // Save as Parquet
        saveHumansToParquet(experiment, humans, 0);

        // Save initial metrics
        Metrics initialMetrics = computeMetrics(humans, 0);
        saveMetricsToParquet(experiment, initialMetrics);

        log.info("Initialized {} humans", humans.size());
    }

    /**
     * Execute a single simulation step.
     * 
     * This is where the core simulation logic runs via Spark DataFrames.
     */
    private void executeSingleStep(Experiment experiment) {
        long currentStep = experiment.getCurrentStep();
        long nextStep = currentStep + 1;
        SimulationParams params = experiment.getParams();

        log.debug("Executing step {} -> {}", currentStep, nextStep);

        // Load current state
        Path statePath = getStatePath(experiment, currentStep);
        Dataset<Row> humansDF = sparkSession.read().parquet(statePath.toString());

        // Seed for deterministic operations
        long stepSeed = params.getSeed() + nextStep;
        Random random = new Random(stepSeed);

        // Convert to list for processing (for simpler logic)
        // In production with millions of rows, keep everything in Spark
        List<Human> humans = humansDF.collectAsList().stream()
                .map(this::rowToHuman)
                .toList();

        // Apply simulation steps
        List<Human> updated = applySimulationStep(humans, params, random);

        // Compute metrics
        Metrics metrics = computeMetrics(updated, nextStep);

        // Save updated state and metrics
        saveHumansToParquet(experiment, updated, nextStep);
        saveMetricsToParquet(experiment, metrics);
    }

    /**
     * Core simulation logic for one step.
     */
    private List<Human> applySimulationStep(List<Human> humans, SimulationParams params, Random random) {
        // Count current mood distribution for contagion
        long joyCount = humans.stream().filter(h -> h.getMood() == Mood.JOY).count();
        long rageCount = humans.stream().filter(h -> h.getMood() == Mood.RAGE).count();
        double population = humans.size();

        // Contagion probabilities
        double joyContagionProb = (joyCount / population) * params.getJoySpreadMultiplier() * 0.1;
        double rageContagionProb = (rageCount / population) * params.getRageSpreadMultiplier() * 0.1;

        List<Human> updated = new ArrayList<>();

        for (Human human : humans) {
            Human.HumanBuilder builder = human.toBuilder();

            // === 1. Emotional Contagion ===
            // Humans can "catch" emotions from the population
            if (human.getMood() == Mood.NEUTRAL) {
                double roll = random.nextDouble();
                if (roll < rageContagionProb) {
                    builder.mood(Mood.RAGE);
                } else if (roll < rageContagionProb + joyContagionProb) {
                    builder.mood(Mood.JOY);
                }
            } else if (human.getMood() == Mood.JOY && random.nextDouble() < rageContagionProb * 0.5) {
                // Joy can turn to rage (half rate)
                builder.mood(Mood.NEUTRAL);
            } else if (human.getMood() == Mood.RAGE && random.nextDouble() < joyContagionProb * 0.3) {
                // Rage is sticky - harder to lose (30% of joy rate)
                builder.mood(Mood.NEUTRAL);
            }

            // === 2. Attention Decay ===
            double newAttention = human.getAttentionSpan() * (1 - params.getAttentionDecayRate());
            // Recovery when attention is low
            if (newAttention < 0.5) {
                newAttention += params.getAttentionRecoveryRate();
            }
            builder.attentionSpan(Math.min(1.0, Math.max(0.0, newAttention)));

            // === 3. Fatigue Accumulation ===
            double newFatigue = human.getFatigue();
            // Fatigue increases with engagement (modeled by addiction)
            if (random.nextDouble() < human.getAddictionCoeff()) {
                newFatigue += params.getFatigueAccumulationRate();
            } else {
                // Recovery when not engaging
                newFatigue -= params.getFatigueRecoveryRate();
            }
            builder.fatigue(Math.min(1.0, Math.max(0.0, newFatigue)));

            updated.add(builder.build());
        }

        return updated;
    }

    /**
     * Compute metrics from population state.
     */
    private Metrics computeMetrics(List<Human> humans, long timeStep) {
        // Mood counts
        Map<String, Long> moodDist = new HashMap<>();
        long joyCount = 0, neutralCount = 0, rageCount = 0;

        double totalAttention = 0;
        double totalFatigue = 0;
        long collapsedCount = 0;
        List<Double> attentions = new ArrayList<>();

        for (Human h : humans) {
            switch (h.getMood()) {
                case JOY -> joyCount++;
                case NEUTRAL -> neutralCount++;
                case RAGE -> rageCount++;
            }
            totalAttention += h.getAttentionSpan();
            totalFatigue += h.getFatigue();
            attentions.add(h.getAttentionSpan());
            if (h.isCollapsed())
                collapsedCount++;
        }

        moodDist.put("JOY", joyCount);
        moodDist.put("NEUTRAL", neutralCount);
        moodDist.put("RAGE", rageCount);

        double n = humans.size();

        // Mood entropy: -Σ(p * log(p))
        double entropy = 0;
        for (long count : new long[] { joyCount, neutralCount, rageCount }) {
            if (count > 0) {
                double p = count / n;
                entropy -= p * Math.log(p) / Math.log(2); // log base 2
            }
        }

        // Dominance ratio
        double maxCount = Math.max(Math.max(joyCount, neutralCount), rageCount);
        double dominance = maxCount / n;

        // Gini coefficient for attention
        double gini = computeGini(attentions);

        // Fatigue P95
        Collections.sort(attentions);
        double[] fatigues = humans.stream().mapToDouble(Human::getFatigue).sorted().toArray();
        double fatigueP95 = fatigues.length > 0 ? fatigues[(int) (fatigues.length * 0.95)] : 0;

        return Metrics.builder()
                .timeStep(timeStep)
                .moodDistribution(moodDist)
                .moodEntropy(entropy)
                .emotionDominanceRatio(dominance)
                .rageReproductionRate(1.0) // Simplified - would need previous step data
                .joyReproductionRate(1.0)
                .attentionGini(gini)
                .averageAttention(totalAttention / n)
                .averageFatigue(totalFatigue / n)
                .fatigueP95(fatigueP95)
                .collapsedCount(collapsedCount)
                .build();
    }

    /**
     * Compute Gini coefficient.
     * 
     * Measures inequality: 0 = perfect equality, 1 = maximum inequality.
     */
    private double computeGini(List<Double> values) {
        if (values.isEmpty())
            return 0;

        Collections.sort(values);
        int n = values.size();
        double sum = 0;
        double cumSum = 0;

        for (int i = 0; i < n; i++) {
            cumSum += values.get(i);
            sum += (2.0 * (i + 1) - n - 1) * values.get(i);
        }

        if (cumSum == 0)
            return 0;
        return sum / (n * cumSum);
    }

    // ========== Parquet I/O ==========

    private void saveHumansToParquet(Experiment experiment, List<Human> humans, long step) {
        Path path = getStatePath(experiment, step);

        // Convert to Dataset
        Dataset<Row> df = sparkSession.createDataFrame(humans, Human.class);
        df.write().mode(SaveMode.Overwrite).parquet(path.toString());
    }

    private void saveMetricsToParquet(Experiment experiment, Metrics metrics) {
        Path path = getMetricsPath(experiment);

        Dataset<Row> df = sparkSession.createDataFrame(List.of(metrics), Metrics.class);
        df.write().mode(SaveMode.Append).parquet(path.toString());
    }

    private Path getStatePath(Experiment experiment, long step) {
        return Paths.get(
                simulationConfig.getStorage().getBasePath(),
                experiment.getStoragePath(),
                "state",
                String.format("step_%05d.parquet", step));
    }

    private Path getMetricsPath(Experiment experiment) {
        return Paths.get(
                simulationConfig.getStorage().getBasePath(),
                experiment.getStoragePath(),
                "metrics");
    }

    private Human rowToHuman(Row row) {
        return Human.builder()
                .humanId(row.getAs("humanId"))
                .mood(Mood.valueOf(row.getAs("mood")))
                .attentionSpan(row.getAs("attentionSpan"))
                .addictionCoeff(row.getAs("addictionCoeff"))
                .reactionProb(row.getAs("reactionProb"))
                .fatigue(row.getAs("fatigue"))
                .build();
    }

    private ExperimentResponse.PopulationSummary loadPopulationSummary(Experiment experiment) {
        Path statePath = getStatePath(experiment, experiment.getCurrentStep());
        Dataset<Row> df = sparkSession.read().parquet(statePath.toString());

        long total = df.count();
        long joy = df.filter("mood = 'JOY'").count();
        long neutral = df.filter("mood = 'NEUTRAL'").count();
        long rage = df.filter("mood = 'RAGE'").count();

        Row stats = df.agg(
                functions.avg("attentionSpan"),
                functions.avg("fatigue")).first();

        return ExperimentResponse.PopulationSummary.builder()
                .totalHumans(total)
                .joyCount(joy)
                .neutralCount(neutral)
                .rageCount(rage)
                .averageAttention(stats.getDouble(0))
                .averageFatigue(stats.getDouble(1))
                .build();
    }

    private List<Metrics> loadMetricsHistory(Experiment experiment) {
        Path metricsPath = getMetricsPath(experiment);

        if (!Files.exists(metricsPath)) {
            return List.of();
        }

        Dataset<Row> df = sparkSession.read().parquet(metricsPath.toString());
        return df.orderBy("timeStep").collectAsList().stream()
                .map(this::rowToMetrics)
                .toList();
    }

    private Metrics rowToMetrics(Row row) {
        return Metrics.builder()
                .timeStep(row.getAs("timeStep"))
                .moodEntropy(row.getAs("moodEntropy"))
                .emotionDominanceRatio(row.getAs("emotionDominanceRatio"))
                .attentionGini(row.getAs("attentionGini"))
                .averageAttention(row.getAs("averageAttention"))
                .averageFatigue(row.getAs("averageFatigue"))
                .fatigueP95(row.getAs("fatigueP95"))
                .collapsedCount(row.getAs("collapsedCount"))
                .build();
    }

    private ExperimentResponse toResponse(Experiment experiment) {
        return ExperimentResponse.builder()
                .experimentId(experiment.getId().toString())
                .name(experiment.getName())
                .status(experiment.getStatus().name())
                .currentStep(experiment.getCurrentStep())
                .params(experiment.getParams())
                .createdAt(experiment.getCreatedAt())
                .updatedAt(experiment.getUpdatedAt())
                .build();
    }
}
