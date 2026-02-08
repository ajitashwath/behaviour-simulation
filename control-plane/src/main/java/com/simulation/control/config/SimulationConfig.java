package com.simulation.control.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the simulation platform.
 * 
 * Loaded from application.yml under the "simulation" prefix.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "simulation")
public class SimulationConfig {

    /**
     * Default simulation parameters.
     */
    private Defaults defaults = new Defaults();

    /**
     * Storage configuration.
     */
    private Storage storage = new Storage();

    /**
     * Spark configuration.
     */
    private Spark spark = new Spark();

    @Data
    public static class Defaults {
        private int populationSize = 10_000;
        private int contentCount = 100;
        private double rageSpreadMultiplier = 1.0;
        private double joySpreadMultiplier = 1.0;
        private double attentionDecayRate = 0.05;
        private double fatigueAccumulationRate = 0.02;
    }

    @Data
    public static class Storage {
        /**
         * Base path for Parquet files.
         * Each experiment creates a subdirectory.
         */
        private String basePath = "./data/experiments";
    }

    @Data
    public static class Spark {
        /**
         * Spark master URL.
         * "local[*]" for development, "spark://host:7077" for cluster.
         */
        private String master = "local[*]";

        /**
         * Spark application name.
         */
        private String appName = "BehaviorSimulation";

        /**
         * Mode for job submission.
         * "in-process" runs Spark within the JVM.
         * "spark-submit" spawns external process.
         */
        private String submitMode = "in-process";

        /**
         * Path to simulation engine JAR (for spark-submit mode).
         */
        private String engineJar = "./simulation-engine/target/simulation-engine-1.0.0-SNAPSHOT.jar";
    }
}
