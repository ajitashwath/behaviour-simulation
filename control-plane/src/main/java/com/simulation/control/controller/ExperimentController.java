package com.simulation.control.controller;

import com.simulation.common.dto.CreateExperimentRequest;
import com.simulation.common.dto.ExperimentResponse;
import com.simulation.common.dto.StepRequest;
import com.simulation.control.service.ExperimentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for experiment management.
 * 
 * Provides endpoints for:
 * - Creating experiments
 * - Executing simulation steps
 * - Retrieving state and metrics
 */
@Slf4j
@RestController
@RequestMapping("/experiments")
@RequiredArgsConstructor
public class ExperimentController {

    private final ExperimentService experimentService;

    /**
     * Create a new experiment.
     * 
     * POST /experiments
     * 
     * Request body:
     * {
     * "name": "rage-divergence-test",
     * "description": "Testing 10% faster rage spread",
     * "params": {
     * "seed": 42,
     * "populationSize": 10000,
     * "rageSpreadMultiplier": 1.1,
     * "joySpreadMultiplier": 1.0
     * }
     * }
     */
    @PostMapping
    public ResponseEntity<ExperimentResponse> createExperiment(
            @Valid @RequestBody CreateExperimentRequest request) {

        log.info("POST /experiments - Creating experiment: {}", request.getName());
        ExperimentResponse response = experimentService.createExperiment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Execute simulation steps.
     * 
     * POST /experiments/{id}/step
     * 
     * Request body (optional):
     * {
     * "steps": 10,
     * "returnMetrics": true
     * }
     */
    @PostMapping("/{id}/step")
    public ResponseEntity<ExperimentResponse> executeStep(
            @PathVariable("id") UUID experimentId,
            @RequestBody(required = false) StepRequest request) {

        // Default to single step if no body provided
        if (request == null) {
            request = new StepRequest();
        }

        log.info("POST /experiments/{}/step - Executing {} steps", experimentId, request.getSteps());
        ExperimentResponse response = experimentService.executeStep(experimentId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current population state.
     * 
     * GET /experiments/{id}/state
     * 
     * Returns experiment info with population summary.
     */
    @GetMapping("/{id}/state")
    public ResponseEntity<ExperimentResponse> getState(@PathVariable("id") UUID experimentId) {
        log.info("GET /experiments/{}/state", experimentId);
        ExperimentResponse response = experimentService.getState(experimentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get metrics history.
     * 
     * GET /experiments/{id}/metrics
     * 
     * Returns experiment info with full metrics timeline.
     */
    @GetMapping("/{id}/metrics")
    public ResponseEntity<ExperimentResponse> getMetrics(@PathVariable("id") UUID experimentId) {
        log.info("GET /experiments/{}/metrics", experimentId);
        ExperimentResponse response = experimentService.getMetrics(experimentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get experiment by ID.
     * 
     * GET /experiments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExperimentResponse> getExperiment(@PathVariable("id") UUID experimentId) {
        log.info("GET /experiments/{}", experimentId);
        ExperimentResponse response = experimentService.getState(experimentId);
        return ResponseEntity.ok(response);
    }

    /**
     * List all experiments.
     * 
     * GET /experiments
     */
    @GetMapping
    public ResponseEntity<List<ExperimentResponse>> listExperiments() {
        log.info("GET /experiments - Listing all experiments");
        List<ExperimentResponse> response = experimentService.listExperiments();
        return ResponseEntity.ok(response);
    }
}
