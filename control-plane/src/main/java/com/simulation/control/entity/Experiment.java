package com.simulation.control.entity;

import com.simulation.common.dto.SimulationParams;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing an experiment.
 * 
 * Stores experiment metadata and configuration.
 * Actual simulation state is stored in Parquet files.
 */
@Entity
@Table(name = "experiments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Experiment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * Current experiment status.
     * 
     * CREATED: Initialized but not started
     * RUNNING: Simulation in progress
     * PAUSED: Temporarily halted
     * COMPLETED: All steps executed
     * FAILED: Error occurred
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ExperimentStatus status = ExperimentStatus.CREATED;

    /**
     * Current simulation step (0-indexed).
     * Increments after each successful step execution.
     */
    @Column(nullable = false)
    @Builder.Default
    private long currentStep = 0L;

    /**
     * Simulation parameters stored as JSON.
     * PostgreSQL uses JSONB, H2 uses JSON.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private SimulationParams params;

    /**
     * Path to Parquet files for this experiment.
     * Relative to the configured storage base path.
     */
    @Column(nullable = false)
    private String storagePath;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    /**
     * Update timestamp before each save.
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        this.updatedAt = this.createdAt;
    }

    /**
     * Experiment lifecycle states.
     */
    public enum ExperimentStatus {
        CREATED,
        RUNNING,
        PAUSED,
        COMPLETED,
        FAILED
    }
}
