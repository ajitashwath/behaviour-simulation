package com.simulation.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new experiment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExperimentRequest {

    /**
     * Human-readable name for this experiment.
     */
    @NotBlank(message = "Experiment name is required")
    @Size(max = 100, message = "Name must be 100 characters or less")
    private String name;

    /**
     * Optional description of the experiment's purpose.
     */
    @Size(max = 500, message = "Description must be 500 characters or less")
    private String description;

    /**
     * Simulation parameters. If null, defaults are used.
     */
    @Valid
    private SimulationParams params;
}
