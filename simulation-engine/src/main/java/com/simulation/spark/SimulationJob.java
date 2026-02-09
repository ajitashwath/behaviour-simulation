package com.simulation.spark;

import com.simulation.common.dto.SimulationParams;
import com.simulation.common.model.*;
import com.simulation.spark.metrics.MetricsAggregator;
import com.simulation.spark.step.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.*;
//import org.apache.spark.sql.types.DataTypes;
//import org.apache.spark.sql.types.StructType;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.apache.spark.sql.functions.*;

/**
 * Main Spark job for running behavior simulations.
 * 
 * This class can be executed via:
 * 1. spark-submit (production)
 * 2. In-process from Spring Boot (development)
 * 
 * All state is stored in Parquet format for durability and efficiency.
 */
@Slf4j
public class SimulationJob implements Serializable {

    private static final long serialVersionUID = 1L;

    private final SparkSession spark;
    private final SimulationParams params;
    private final String storagePath;

    // Simulation step processors
    private final ExposureStep exposureStep;
    private final ReactionStep reactionStep;
    private final MoodContagionStep moodContagionStep;
    private final AttentionDecayStep attentionDecayStep;
    private final FatigueStep fatigueStep;
    private final MetricsAggregator metricsAggregator;

    public SimulationJob(SparkSession spark, SimulationParams params, String storagePath) {
        this.spark = spark;
        this.params = params;
        this.storagePath = storagePath;

        // Initialize step processors
        this.exposureStep = new ExposureStep(params);
        this.reactionStep = new ReactionStep(params);
        this.moodContagionStep = new MoodContagionStep(params);
        this.attentionDecayStep = new AttentionDecayStep(params);
        this.fatigueStep = new FatigueStep(params);
        this.metricsAggregator = new MetricsAggregator();
    }

    /**
     * Entry point for spark-submit execution.
     * 
     * Arguments:
     * [0] - storage path
     * [1] - current step number
     * [2] - seed
     * [3] - population size
     * [4] - rage spread multiplier
     * [5] - joy spread multiplier
     */
    public static void main(String[] args) {
        if (args.length < 6) {
            System.err.println("Usage: SimulationJob <storagePath> <step> <seed> <pop> <rageMulti> <joyMulti>");
            System.exit(1);
        }

        String storagePath = args[0];
        long currentStep = Long.parseLong(args[1]);

        SimulationParams params = SimulationParams.builder()
                .seed(Long.parseLong(args[2]))
                .populationSize(Integer.parseInt(args[3]))
                .rageSpreadMultiplier(Double.parseDouble(args[4]))
                .joySpreadMultiplier(Double.parseDouble(args[5]))
                .build();

        SparkSession spark = SparkSession.builder()
                .appName("BehaviorSimulation-Step-" + currentStep)
                .getOrCreate();

        try {
            SimulationJob job = new SimulationJob(spark, params, storagePath);
            job.executeStep(currentStep);
        } finally {
            spark.stop();
        }
    }

    /**
     * Execute a single simulation step.
     * 
     * This is the core simulation loop:
     * 1. Load current state
     * 2. Compute content exposure
     * 3. Calculate reactions
     * 4. Update moods via contagion
     * 5. Apply attention decay
     * 6. Accumulate fatigue
     * 7. Aggregate metrics
     * 8. Persist new state
     */
    public void executeStep(long currentStep) {
        log.info("Executing simulation step: {}", currentStep);
        long nextStep = currentStep + 1;

        // Seed for this step (deterministic)
        long stepSeed = params.getSeed() + nextStep;

        // Load current state
        Dataset<Row> humansDF = loadState(currentStep);
        Dataset<Row> contentDF = loadOrCreateContent(currentStep);

        // Cache for multiple operations
        humansDF.cache();
        contentDF.cache();

        // === SIMULATION PIPELINE ===

        // Step 1: Compute exposure (which humans see which content)
        Dataset<Row> exposuresDF = exposureStep.compute(humansDF, contentDF, currentStep, stepSeed);

        // Step 2: Calculate reactions
        Dataset<Row> reactionsDF = reactionStep.compute(exposuresDF, stepSeed);

        // Step 3: Update moods via emotional contagion
        Dataset<Row> moodUpdatedDF = moodContagionStep.apply(humansDF, reactionsDF, stepSeed);

        // Step 4: Apply attention decay and recovery
        Dataset<Row> attentionUpdatedDF = attentionDecayStep.apply(moodUpdatedDF);

        // Step 5: Accumulate fatigue
        Dataset<Row> fatigueUpdatedDF = fatigueStep.apply(attentionUpdatedDF, reactionsDF);

        // Step 6: Aggregate metrics
        Metrics metrics = metricsAggregator.compute(fatigueUpdatedDF, reactionsDF, nextStep);

        // === PERSIST RESULTS ===

        // Save updated human state
        saveState(fatigueUpdatedDF, nextStep);

        // Save metrics
        saveMetrics(metrics);

        // Save interactions for analysis
        saveInteractions(reactionsDF, nextStep);

        // Cleanup
        humansDF.unpersist();
        contentDF.unpersist();

        log.info("Step {} complete. Joy: {}, Neutral: {}, Rage: {}",
                nextStep,
                metrics.getMoodDistribution().get("JOY"),
                metrics.getMoodDistribution().get("NEUTRAL"),
                metrics.getMoodDistribution().get("RAGE"));
    }

    /**
     * Initialize population for a new experiment.
     */
    public void initializePopulation() {
        log.info("Initializing population of {} humans", params.getPopulationSize());

        // Create DataFrame with initial humans
        Dataset<Row> humansDF = createInitialPopulation();

        // Save as step 0
        saveState(humansDF, 0);

        // Create initial content
        Dataset<Row> contentDF = createInitialContent();
        saveContent(contentDF);

        // Compute initial metrics
        Metrics metrics = metricsAggregator.compute(humansDF, spark.emptyDataFrame(), 0);
        saveMetrics(metrics);

        log.info("Initialization complete");
    }

    // ========== State Management ==========

    private Dataset<Row> loadState(long step) {
        Path path = Paths.get(storagePath, "state", String.format("step_%05d.parquet", step));
        return spark.read().parquet(path.toString());
    }

    private void saveState(Dataset<Row> df, long step) {
        Path path = Paths.get(storagePath, "state", String.format("step_%05d.parquet", step));
        df.write().mode(SaveMode.Overwrite).parquet(path.toString());
    }

    private Dataset<Row> loadOrCreateContent(long step) {
        Path path = Paths.get(storagePath, "content", "content.parquet");
        if (Files.exists(path)) {
            return spark.read().parquet(path.toString());
        }
        return createInitialContent();
    }

    private void saveContent(Dataset<Row> df) {
        Path path = Paths.get(storagePath, "content", "content.parquet");
        df.write().mode(SaveMode.Overwrite).parquet(path.toString());
    }

    private void saveMetrics(Metrics metrics) {
        Path path = Paths.get(storagePath, "metrics");
        Dataset<Row> df = spark.createDataFrame(java.util.List.of(metrics), Metrics.class);
        df.write().mode(SaveMode.Append).parquet(path.toString());
    }

    private void saveInteractions(Dataset<Row> df, long step) {
        Path path = Paths.get(storagePath, "interactions", String.format("step_%05d.parquet", step));
        df.write().mode(SaveMode.Overwrite).parquet(path.toString());
    }

    // ========== Initialization ==========

    private Dataset<Row> createInitialPopulation() {
        // Generate humans using Spark SQL
        Dataset<Row> rangeDF = spark.range(0, params.getPopulationSize())
                .withColumnRenamed("id", "humanId");

        // Add random traits using deterministic seed
        long seed = params.getSeed();
        double joyFrac = params.getInitialMoodDistribution()[0];
        double neutralFrac = params.getInitialMoodDistribution()[1];

        return rangeDF
                // Determine initial mood based on distribution
                .withColumn("moodRoll", rand(seed))
                .withColumn("mood",
                        when(col("moodRoll").lt(joyFrac), lit("JOY"))
                                .when(col("moodRoll").lt(joyFrac + neutralFrac), lit("NEUTRAL"))
                                .otherwise(lit("RAGE")))
                // Random traits
                .withColumn("attentionSpan", lit(0.5).plus(rand(seed + 1).multiply(0.5)))
                .withColumn("addictionCoeff", lit(0.3).plus(rand(seed + 2).multiply(0.5)))
                .withColumn("reactionProb", lit(0.3).plus(rand(seed + 3).multiply(0.4)))
                .withColumn("fatigue", rand(seed + 4).multiply(0.1))
                // Cleanup
                .drop("moodRoll");
    }

    private Dataset<Row> createInitialContent() {
        Dataset<Row> rangeDF = spark.range(0, params.getContentCount())
                .withColumnRenamed("id", "contentId");

        long seed = params.getSeed() + 1000;

        return rangeDF
                // Assign emotion types (roughly equal distribution)
                .withColumn("typeRoll", rand(seed))
                .withColumn("emotionType",
                        when(col("typeRoll").lt(0.33), lit("POSITIVE"))
                                .when(col("typeRoll").lt(0.66), lit("NEUTRAL"))
                                .otherwise(lit("NEGATIVE")))
                // Content properties
                .withColumn("intensity", lit(0.3).plus(rand(seed + 1).multiply(0.6)))
                .withColumn("halfLife", lit(5.0).plus(rand(seed + 2).multiply(15.0)))
                .withColumn("createdAtStep", lit(0L))
                .drop("typeRoll");
    }
}
