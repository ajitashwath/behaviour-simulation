package com.simulation.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepRequest {

    @Builder.Default
    private int steps = 1;

    @Builder.Default
    private boolean returnMetrics = true;
}
