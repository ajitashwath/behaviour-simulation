package com.simulation.control.config;

import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Configuration for Apache Spark integration.
 * 
 * Creates a Spark session for reading Parquet files and
 * executing simulation jobs in-process.
 */
@Configuration
public class SparkConfig {

    private final SimulationConfig simulationConfig;

    public SparkConfig(SimulationConfig simulationConfig) {
        this.simulationConfig = simulationConfig;
    }

    /**
     * Create Spark session.
     * 
     * Marked as @Lazy because Spark initialization is expensive
     * and we only need it when actually running simulations.
     */
    @Bean
    @Lazy
    public SparkSession sparkSession() {
        return SparkSession.builder()
                .appName(simulationConfig.getSpark().getAppName())
                .master(simulationConfig.getSpark().getMaster())
                // Reduce logging verbosity
                .config("spark.ui.showConsoleProgress", "false")
                // Optimize for local mode
                .config("spark.sql.shuffle.partitions", "4")
                // Enable Parquet support
                .config("spark.sql.parquet.compression.codec", "snappy")
                .getOrCreate();
    }
}
