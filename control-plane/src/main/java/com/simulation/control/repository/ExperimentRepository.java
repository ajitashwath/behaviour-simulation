package com.simulation.control.repository;

import com.simulation.control.entity.Experiment;
import com.simulation.control.entity.Experiment.ExperimentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for experiment persistence operations.
 */
@Repository
public interface ExperimentRepository extends JpaRepository<Experiment, UUID> {

    /**
     * Find all experiments by status.
     */
    List<Experiment> findByStatus(ExperimentStatus status);

    /**
     * Find experiments by name (case-insensitive partial match).
     */
    List<Experiment> findByNameContainingIgnoreCase(String name);

    /**
     * Find experiments ordered by creation time (newest first).
     */
    List<Experiment> findAllByOrderByCreatedAtDesc();
}
