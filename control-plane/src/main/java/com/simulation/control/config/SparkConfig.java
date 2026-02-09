package com.simulation.control.config;

import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.io.File;
import java.nio.file.Paths;

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
        // Set HADOOP_HOME for Windows compatibility if not already set
        if (System.getProperty("hadoop.home.dir") == null && System.getenv("HADOOP_HOME") == null) {
            try {
                String hadoopHome = Paths.get("..", "hadoop").toAbsolutePath().toString();
                System.setProperty("hadoop.home.dir", hadoopHome);
                // Also add bin to path for hadoop.dll
                String binPath = Paths.get(hadoopHome, "bin").toString();
                String existingPath = System.getProperty("java.library.path");
                if (existingPath == null || !existingPath.contains(binPath)) {
                    System.setProperty("java.library.path",
                            binPath + File.pathSeparator + (existingPath == null ? "" : existingPath));
                }

                // Explicitly load hadoop.dll to resolve UnsatisfiedLinkError
                File dllFile = new File(binPath, "hadoop.dll");
                if (dllFile.exists()) {
                    System.load(dllFile.getAbsolutePath());
                }
            } catch (Exception e) {
                // Ignore errors in setting path
            }
        }

        return SparkSession.builder()
                .appName(simulationConfig.getSpark().getAppName())
                .master(simulationConfig.getSpark().getMaster())
                // Reduce logging verbosity
                .config("spark.ui.showConsoleProgress", "false")
                // Disable UI to avoid servlet conflicts with Spring Boot 3
                .config("spark.ui.enabled", "false")
                // Optimize for local mode
                .config("spark.sql.shuffle.partitions", "4")
                // Enable Parquet support
                .config("spark.sql.parquet.compression.codec", "snappy")
                .getOrCreate();
    }
}
