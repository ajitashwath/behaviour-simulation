package com.simulation.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for executing simulation steps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepRequest {

    /**
     * Number of steps to execute.
     * Default: 1
     */
    @Builder.Default
    private int steps = 1;

    /**
     * Whether to return detailed metrics after stepping.
     * Default: true
     */
    @Builder.Default
    private boolean returnMetrics = true;
}
