package com.simulation.control;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the Behavior Simulation Control Plane.
 * 
 * This application provides REST APIs for:
 * - Creating and managing experiments
 * - Triggering Spark simulation jobs
 * - Retrieving simulation state and metrics
 * 
 * The actual simulation logic runs in Apache Spark.
 * This control plane only manages lifecycle and orchestration.
 */
@SpringBootApplication
@EnableAsync  // Enable async execution for Spark job submission
public class ControlPlaneApplication {

    public static void main(String[] args) {
        SpringApplication.run(ControlPlaneApplication.class, args);
    }
}
