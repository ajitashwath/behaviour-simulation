package com.simulation.control.repository;

import com.simulation.control.entity.Experiment;
import com.simulation.control.entity.Experiment.ExperimentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExperimentRepository extends JpaRepository<Experiment, UUID> {

    List<Experiment> findByStatus(ExperimentStatus status);

    List<Experiment> findByNameContainingIgnoreCase(String name);

    List<Experiment> findAllByOrderByCreatedAtDesc();
}
